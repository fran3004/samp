package co.edu.upc.samp.servicios;

import co.edu.upc.samp.datos.jpa.OfferJpaRepository;
import co.edu.upc.samp.datos.jpa.PriceHistoryJpaRepository;
import co.edu.upc.samp.datos.jpa.ProductJpaRepository;
import co.edu.upc.samp.datos.jpa.StoreJpaRepository;
import co.edu.upc.samp.entidades.Money;
import co.edu.upc.samp.entidades.Offer;
import co.edu.upc.samp.entidades.PriceHistory;
import co.edu.upc.samp.entidades.Product;
import co.edu.upc.samp.entidades.ProductImage;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.StoreProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Servicio de acceso al catálogo persistido en PostgreSQL/Supabase mediante Spring Data JPA.
 *
 * <p>Todos los repositorios JPA son opcionales ({@code required = false}) para que SAMP pueda
 * ejecutarse tanto conectado a Supabase (52 tablas con Flyway) como en modo local sin base de datos
 * o en modo consola ({@code ArrayList} + JSON).
 */
@Service
public class DatabaseCatalogService {

    private final OfferJpaRepository offerJpaRepository;
    private final PriceHistoryJpaRepository priceHistoryJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final StoreJpaRepository storeJpaRepository;

    /**
     * Resumen del estado de persistencia en base de datos para el panel administrativo.
     *
     * @param connected          {@code true} si JPA está conectado a PostgreSQL/Supabase
     * @param storesCount        cantidad de tiendas registradas en {@code stores}
     * @param productsCount      cantidad de productos canónicos en {@code products}
     * @param activeOffersCount  ofertas vigentes en {@code offers}
     * @param historyPointsCount puntos históricos en {@code price_history}
     */
    public record DatabaseSummary(
            boolean connected,
            long storesCount,
            long productsCount,
            long activeOffersCount,
            long historyPointsCount
    ) {
    }

    /**
     * Construye el servicio inyectando los repositorios JPA solo cuando la base de datos está habilitada.
     *
     * @param offerJpaRepository        repositorio de ofertas
     * @param priceHistoryJpaRepository repositorio de historial de precios
     * @param productJpaRepository      repositorio de productos canónicos
     * @param storeJpaRepository        repositorio de tiendas
     */
    @Autowired
    public DatabaseCatalogService(
            @Autowired(required = false) OfferJpaRepository offerJpaRepository,
            @Autowired(required = false) PriceHistoryJpaRepository priceHistoryJpaRepository,
            @Autowired(required = false) ProductJpaRepository productJpaRepository,
            @Autowired(required = false) StoreJpaRepository storeJpaRepository
    ) {
        this.offerJpaRepository = offerJpaRepository;
        this.priceHistoryJpaRepository = priceHistoryJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.storeJpaRepository = storeJpaRepository;
    }

    /**
     * Indica si el backend está conectado a PostgreSQL/Supabase real.
     *
     * @return {@code true} si los repositorios JPA están activos
     */
    public boolean isDatabaseActive() {
        return offerJpaRepository != null && storeJpaRepository != null && productJpaRepository != null;
    }

    /**
     * Consulta las ofertas vigentes almacenadas en Supabase para una tienda y consulta dadas.
     *
     * @param storeCode código de tienda (ej. {@code MERCADOLIBRE_CO})
     * @param query     consulta por texto o GTIN
     * @return lista de ofertas normalizadas desde la base de datos (vacía si no hay BD activa)
     */
    public List<RawOffer> findOffersForStore(String storeCode, SearchQuery query) {
        if (!isDatabaseActive()) {
            return List.of();
        }
        String normalizedCode = storeCode == null ? "" : storeCode.trim().toUpperCase(Locale.ROOT);
        return offerJpaRepository.findByCurrentTrueOrderByFinalPriceRefAsc().stream()
                .filter(o -> o.getStoreProduct() != null && o.getStoreProduct().getStore() != null)
                .filter(o -> o.getStoreProduct().getStore().getCode().equalsIgnoreCase(normalizedCode))
                .filter(o -> matchesQuery(o, query))
                .map(this::toRawOffer)
                .toList();
    }

    /**
     * Recupera la serie histórica diaria (mínimo por día en COP) desde {@code price_history}
     * para un producto identificado por GTIN.
     *
     * @param gtinOrKey       código GTIN del producto
     * @param priceCalculator calculadora para normalizar montos a COP
     * @return serie ordenada cronológicamente o lista vacía si no hay historial en BD
     */
    public List<BuyOrWaitAdvisor.PricePoint> findPriceHistory30Days(String gtinOrKey, PriceCalculator priceCalculator) {
        if (!isDatabaseActive() || priceHistoryJpaRepository == null || gtinOrKey == null || gtinOrKey.isBlank()) {
            return List.of();
        }
        return productJpaRepository.findByIdentifierValue(gtinOrKey.trim())
                .map(product -> aggregateDailyHistory(product, priceCalculator))
                .orElse(List.of());
    }

    /**
     * Devuelve el conteo real de registros en Supabase para mostrarlo en el panel de administración.
     *
     * @return resumen del estado de persistencia
     */
    public DatabaseSummary getSummary() {
        if (!isDatabaseActive()) {
            return new DatabaseSummary(false, 0, 0, 0, 0);
        }
        long stores = storeJpaRepository.count();
        long products = productJpaRepository.count();
        long activeOffers = offerJpaRepository.findByCurrentTrueOrderByFinalPriceRefAsc().size();
        long history = priceHistoryJpaRepository == null ? 0 : priceHistoryJpaRepository.count();
        return new DatabaseSummary(true, stores, products, activeOffers, history);
    }

    private List<BuyOrWaitAdvisor.PricePoint> aggregateDailyHistory(Product product, PriceCalculator priceCalculator) {
        List<PriceHistory> rows = priceHistoryJpaRepository.findByStoreProductProductIdOrderByRecordedAtAsc(product.getId());
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, BigDecimal> lowestPerDay = new TreeMap<>();
        for (PriceHistory row : rows) {
            LocalDate day = row.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate();
            BigDecimal totalOriginal = row.getPrice().add(row.getShipping() == null ? BigDecimal.ZERO : row.getShipping());
            BigDecimal copAmount = priceCalculator
                    .convert(new Money(totalOriginal, row.getCurrency().trim()), "COP", null)
                    .getAmount()
                    .setScale(0, RoundingMode.HALF_UP);
            lowestPerDay.merge(day, copAmount, BigDecimal::min);
        }
        return lowestPerDay.entrySet().stream()
                .map(e -> new BuyOrWaitAdvisor.PricePoint(e.getKey(), e.getValue()))
                .toList();
    }

    private boolean matchesQuery(Offer offer, SearchQuery query) {
        if (query == null) {
            return true;
        }
        StoreProduct sp = offer.getStoreProduct();
        Product product = sp.getProduct();
        String gtin = extractGtin(sp, product);

        if (query.hasGtin()) {
            return gtin != null && gtin.equalsIgnoreCase(query.gtin().trim());
        }
        if (query.text() == null || query.text().isBlank()) {
            return true;
        }
        String term = query.text().trim().toLowerCase(Locale.ROOT);
        String searchable = String.join(" ",
                safe(sp.getTitle()),
                safe(sp.getStore().getName()),
                safe(product == null ? null : product.getName()),
                safe(product == null ? null : product.getModel()),
                safe(product == null || product.getBrand() == null ? null : product.getBrand().getName()),
                safe(product == null || product.getCategory() == null ? null : product.getCategory().getName()),
                safe(gtin)
        ).toLowerCase(Locale.ROOT);
        for (String token : term.split("\\s+")) {
            if (!token.isBlank() && !searchable.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private RawOffer toRawOffer(Offer offer) {
        StoreProduct sp = offer.getStoreProduct();
        Store store = sp.getStore();
        Product product = sp.getProduct();
        String brand = product != null && product.getBrand() != null ? product.getBrand().getName() : null;
        String model = product != null ? product.getModel() : null;
        String gtin = extractGtin(sp, product);
        List<String> images = product == null
                ? List.of()
                : product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getPosition))
                .map(ProductImage::getUrl)
                .toList();

        return new RawOffer(
                store.getCode(),
                sp.getExternalId(),
                sp.getTitle(),
                offer.getSourceUrl() != null ? offer.getSourceUrl() : sp.getUrl(),
                offer.getPriceAmount(),
                offer.getCurrencyCode().trim(),
                offer.getShippingAmount(),
                offer.isShippingKnown(),
                offer.getTaxAmount(),
                offer.isTaxKnown(),
                offer.getAvailability(),
                brand,
                model,
                gtin,
                store.getDataKind(),
                images,
                Map.of("fuente", "Supabase PostgreSQL"),
                offer.getCheckedAt()
        );
    }

    private String extractGtin(StoreProduct sp, Product product) {
        if (sp.getGtinRaw() != null && !sp.getGtinRaw().isBlank()) {
            return sp.getGtinRaw().trim();
        }
        if (product != null && !product.getIdentifiers().isEmpty()) {
            return product.getIdentifiers().getFirst().getValue();
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
