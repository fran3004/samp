package co.edu.upc.samp.servicios;

import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.Money;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.entidades.excepciones.ConnectorException;
import co.edu.upc.samp.servicios.conectores.CatalogFallbackConnector;
import co.edu.upc.samp.servicios.conectores.ScrapingStoreConnector;
import co.edu.upc.samp.servicios.emparejamiento.MatchStrategy;
import co.edu.upc.samp.servicios.emparejamiento.ProductMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Servicio central de búsqueda paralela y comparación transparente entre las 5 tiendas de Colombia.
 *
 * <p>Intenta consultar cada conector de scraping respetando {@code robots.txt}. Si una tienda
 * bloquea o no devuelve resultados en tiempo, activa automáticamente {@link CatalogFallbackConnector}
 * marcando las ofertas con {@link DataKind#DEMO} y reportando el estado real de cada fuente.
 */
@Service
public class SearchService {

    private final List<ScrapingStoreConnector> scrapingConnectors;
    private final CatalogFallbackConnector fallbackConnector;
    private final PriceCalculator priceCalculator;
    private final ProductMatcher productMatcher;
    private final BuyOrWaitAdvisor buyOrWaitAdvisor;
    private final DatabaseCatalogService databaseCatalogService;

    /**
     * Inicializa el servicio de búsqueda con conectores, respaldo DEMO, motor de precios y catálogo JPA opcional.
     *
     * @param scrapingConnectors     conectores concretos de las 5 tiendas
     * @param fallbackConnector      conector de respaldo transparente DEMO
     * @param priceCalculator        calculadora de precio final y conversión de moneda
     * @param productMatcher         motor de emparejamiento verificable
     * @param buyOrWaitAdvisor       asesor de historial de 30 días
     * @param databaseCatalogService servicio opcional conectado a PostgreSQL/Supabase
     */
    @Autowired
    public SearchService(
            List<ScrapingStoreConnector> scrapingConnectors,
            CatalogFallbackConnector fallbackConnector,
            PriceCalculator priceCalculator,
            ProductMatcher productMatcher,
            BuyOrWaitAdvisor buyOrWaitAdvisor,
            @Autowired(required = false) DatabaseCatalogService databaseCatalogService
    ) {
        this.scrapingConnectors = scrapingConnectors;
        this.fallbackConnector = fallbackConnector;
        this.priceCalculator = priceCalculator;
        this.productMatcher = productMatcher;
        this.buyOrWaitAdvisor = buyOrWaitAdvisor;
        this.databaseCatalogService = databaseCatalogService;
    }

    /**
     * Sobrecarga de constructor para pruebas unitarias sin conexión a base de datos.
     *
     * @param scrapingConnectors conectores concretos de las 5 tiendas
     * @param fallbackConnector  conector de respaldo transparente DEMO
     * @param priceCalculator    calculadora de precio final y conversión de moneda
     * @param productMatcher     motor de emparejamiento verificable
     * @param buyOrWaitAdvisor   asesor de historial de 30 días
     */
    public SearchService(
            List<ScrapingStoreConnector> scrapingConnectors,
            CatalogFallbackConnector fallbackConnector,
            PriceCalculator priceCalculator,
            ProductMatcher productMatcher,
            BuyOrWaitAdvisor buyOrWaitAdvisor
    ) {
        this(scrapingConnectors, fallbackConnector, priceCalculator, productMatcher, buyOrWaitAdvisor, null);
    }

    /**
     * Estado transparente de ejecución por tienda para mostrar fallos parciales en la interfaz.
     *
     * @param storeCode  código de la tienda
     * @param storeName  nombre legible de la tienda
     * @param dataKind   origen de datos efectivo ({@code REAL} o {@code DEMO})
     * @param usedBackup {@code true} si se activó el respaldo DEMO
     * @param itemCount  cantidad de ofertas aportadas
     * @param note       mensaje explicativo en español
     */
    public record StoreQueryStatus(
            String storeCode,
            String storeName,
            DataKind dataKind,
            boolean usedBackup,
            int itemCount,
            String note
    ) {
    }

    /**
     * Oferta normalizada lista para comparación y visualización transparente.
     *
     * @param id                identificador estable de presentación
     * @param storeCode         código de la tienda
     * @param storeName         nombre legible de la tienda
     * @param title             título del producto
     * @param brand             marca si está disponible
     * @param model             modelo si está disponible
     * @param gtin              código GTIN/EAN si existe
     * @param productUrl        enlace externo directo a la tienda
     * @param imageUrl          URL de fotografía del producto
     * @param originalPrice     precio publicado en moneda original
     * @param shippingCost      envío conocido o {@code null} si es desconocido
     * @param shippingKnown     indica si el costo de envío es conocido
     * @param taxAmount         impuesto conocido o {@code null} si es desconocido
     * @param taxKnown          indica si los impuestos son conocidos
     * @param totalInTarget     total conocido convertido a la moneda destino (COP por defecto)
     * @param completeTotal     {@code true} solo si envío e impuestos son conocidos
     * @param comparisonLabel   etiqueta honesta («Total estimado» o «Menor precio publicado»)
     * @param dataKind          {@code REAL} o {@code DEMO}
     * @param availability      disponibilidad reportada
     * @param fetchedAt         fecha/hora exacta de observación
     * @param attributes        atributos relevantes de la tienda
     */
    public record ComparedOffer(
            String id,
            String storeCode,
            String storeName,
            String title,
            String brand,
            String model,
            String gtin,
            String productUrl,
            String imageUrl,
            Money originalPrice,
            Money shippingCost,
            boolean shippingKnown,
            Money taxAmount,
            boolean taxKnown,
            Money totalInTarget,
            boolean completeTotal,
            String comparisonLabel,
            DataKind dataKind,
            String availability,
            Instant fetchedAt,
            Map<String, String> attributes
    ) {
    }

    /**
     * Grupo de ofertas equivalentes (o similares) ordenadas por total estimado.
     *
     * @param groupKey        clave del grupo (GTIN o identificador normalizado)
     * @param canonicalTitle  título principal del producto
     * @param brand           marca del producto
     * @param gtin            código verificable si existe
     * @param imageUrl        imagen principal
     * @param exactMatch      {@code true} si todas las ofertas comparten regla verificable; {@code false} si son similares
     * @param matchBadge      texto transparente («Mismo producto verificado» o «Productos similares»)
     * @param offers          ofertas ordenadas (completas primero por menor total, incompletas señaladas)
     * @param bestOffer       mejor oferta encabezando el grupo
     * @param buyAdvice       recomendación histórica de 30 días
     */
    public record ProductComparisonGroup(
            String groupKey,
            String canonicalTitle,
            String brand,
            String gtin,
            String imageUrl,
            boolean exactMatch,
            String matchBadge,
            List<ComparedOffer> offers,
            ComparedOffer bestOffer,
            BuyOrWaitAdvisor.BuyAdvice buyAdvice
    ) {
    }

    /**
     * Respuesta completa de una búsqueda en SAMP.
     *
     * @param query          consulta ejecutada
     * @param groups         grupos comparados de productos
     * @param storeStatuses  estado individual de las 5 tiendas (para avisos de fallo parcial)
     * @param totalOffers    total de ofertas comparadas
     * @param executedAt     instante de ejecución
     */
    public record SearchResponse(
            SearchQuery query,
            List<ProductComparisonGroup> groups,
            List<StoreQueryStatus> storeStatuses,
            int totalOffers,
            Instant executedAt
    ) {
    }

    /**
     * Ejecuta una búsqueda en paralelo sobre las 5 tiendas configuradas.
     *
     * @param query       consulta por texto o GTIN
     * @param allowLive   si es {@code true}, intenta scraping respetando {@code robots.txt} antes del respaldo DEMO
     * @return resultados agrupados, ordenados y con transparencia por tienda
     */
    public SearchResponse search(SearchQuery query, boolean allowLive) {
        SearchQuery safeQuery = query == null ? SearchQuery.byText("") : query;
        List<RawOffer> allRaw = new ArrayList<>();
        List<StoreQueryStatus> statuses = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, scrapingConnectors.size()))) {
            List<CompletableFuture<StoreResult>> futures = scrapingConnectors.stream()
                    .map(connector -> CompletableFuture.supplyAsync(() -> querySingleStore(connector, safeQuery, allowLive), executor))
                    .toList();

            for (CompletableFuture<StoreResult> future : futures) {
                try {
                    StoreResult result = future.get(9, TimeUnit.SECONDS);
                    allRaw.addAll(result.offers());
                    statuses.add(result.status());
                } catch (Exception e) {
                    statuses.add(new StoreQueryStatus("UNKNOWN", "Tienda", DataKind.DEMO, true, 0,
                            "Tiempo de espera agotado al consultar la tienda."));
                }
            }
        }

        List<ProductComparisonGroup> groups = groupAndRankOffers(allRaw, safeQuery.targetCurrency());
        return new SearchResponse(safeQuery, groups, statuses, allRaw.size(), Instant.now());
    }

    /**
     * Sobrecarga de conveniencia que ejecuta la búsqueda habilitando el modo en vivo con respaldo automático.
     *
     * @param query consulta de búsqueda
     * @return respuesta comparada
     */
    public SearchResponse search(SearchQuery query) {
        return search(query, true);
    }

    /**
     * Busca un grupo de comparación específico por su clave (GTIN o ID de oferta).
     *
     * @param groupKey       clave del grupo
     * @param targetCurrency moneda de visualización
     * @return grupo encontrado o {@code null}
     */
    public ProductComparisonGroup findGroupByKey(String groupKey, String targetCurrency) {
        SearchResponse response = search(new SearchQuery("", "", "", targetCurrency, 50), false);
        return response.groups().stream()
                .filter(g -> g.groupKey().equalsIgnoreCase(groupKey)
                        || (g.gtin() != null && g.gtin().equalsIgnoreCase(groupKey)))
                .findFirst()
                .orElse(null);
    }

    private StoreResult querySingleStore(ScrapingStoreConnector connector, SearchQuery query, boolean allowLive) {
        String code = connector.storeCode();
        String name = friendlyStoreName(code);
        if (allowLive && (!query.text().isBlank() || query.hasGtin())) {
            try {
                List<RawOffer> liveOffers = connector.search(query);
                if (!liveOffers.isEmpty()) {
                    return new StoreResult(liveOffers, new StoreQueryStatus(
                            code, name, DataKind.REAL, false, liveOffers.size(),
                            "Datos consultados en vivo respetando robots.txt."
                    ));
                }
            } catch (ConnectorException ex) {
                return resolvePersistedOrDemo(code, name, query,
                        "Respaldo activo (" + ex.getMessage() + ").");
            }
        }
        return resolvePersistedOrDemo(code, name, query, null);
    }

    private StoreResult resolvePersistedOrDemo(String code, String name, SearchQuery query, String liveFailureNote) {
        if (databaseCatalogService != null && databaseCatalogService.isDatabaseActive()) {
            List<RawOffer> dbOffers = databaseCatalogService.findOffersForStore(code, query);
            DataKind kind = dbOffers.isEmpty() ? DataKind.DEMO : dbOffers.getFirst().dataKind();
            String note = liveFailureNote != null
                    ? liveFailureNote + " Leyendo Supabase PostgreSQL."
                    : (dbOffers.isEmpty() ? "Sin coincidencias en Supabase." : "Catálogo persistido en Supabase PostgreSQL.");
            return new StoreResult(dbOffers, new StoreQueryStatus(code, name, kind, kind == DataKind.DEMO, dbOffers.size(), note));
        }
        List<RawOffer> backup = fallbackConnector.searchForStore(code, query);
        String note = liveFailureNote != null
                ? liveFailureNote
                : (backup.isEmpty() ? "Sin coincidencias en catálogo DEMO." : "Catálogo DEMO verificado.");
        return new StoreResult(backup, new StoreQueryStatus(code, name, DataKind.DEMO, true, backup.size(), note));
    }

    private List<ProductComparisonGroup> groupAndRankOffers(List<RawOffer> rawOffers, String targetCurrency) {
        Map<String, List<RawOffer>> buckets = new LinkedHashMap<>();
        Map<String, Boolean> exactFlags = new LinkedHashMap<>();

        for (RawOffer offer : rawOffers) {
            String assignedKey = null;
            boolean assignedExact = false;

            for (Map.Entry<String, List<RawOffer>> entry : buckets.entrySet()) {
                RawOffer anchor = entry.getValue().getFirst();
                var decisionOpt = productMatcher.match(anchor, offer);
                if (decisionOpt.isPresent()) {
                    MatchStrategy.MatchDecision decision = decisionOpt.get();
                    assignedKey = entry.getKey();
                    assignedExact = decision.exactEquivalent();
                    break;
                }
            }

            if (assignedKey == null) {
                assignedKey = (offer.gtin() != null && !offer.gtin().isBlank())
                        ? offer.gtin()
                        : offer.storeCode() + "-" + offer.externalId();
                buckets.put(assignedKey, new ArrayList<>(List.of(offer)));
                exactFlags.put(assignedKey, offer.gtin() != null && !offer.gtin().isBlank());
            } else {
                buckets.get(assignedKey).add(offer);
                exactFlags.put(assignedKey, Boolean.TRUE.equals(exactFlags.get(assignedKey)) && assignedExact);
            }
        }

        List<ProductComparisonGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<RawOffer>> entry : buckets.entrySet()) {
            List<ComparedOffer> compared = entry.getValue().stream()
                    .map(raw -> toComparedOffer(raw, targetCurrency))
                    .sorted(Comparator
                            .comparing(ComparedOffer::completeTotal, Comparator.reverseOrder())
                            .thenComparing(o -> o.totalInTarget().getAmount()))
                    .toList();

            ComparedOffer best = compared.getFirst();
            boolean exact = Boolean.TRUE.equals(exactFlags.get(entry.getKey()));
            String badge = compared.size() == 1
                    ? "Oferta única encontrada"
                    : (exact ? "Mismo producto verificado" : "Productos similares");

            BigDecimal bestCop = priceCalculator.convert(best.totalInTarget(), "COP", null).getAmount();
            List<BuyOrWaitAdvisor.PricePoint> history = (databaseCatalogService != null && databaseCatalogService.isDatabaseActive())
                    ? databaseCatalogService.findPriceHistory30Days(best.gtin() != null ? best.gtin() : entry.getKey(), priceCalculator)
                    : List.of();
            if (history.isEmpty()) {
                history = buyOrWaitAdvisor.buildDemoHistory30Days(entry.getKey(), bestCop);
            }
            var advice = buyOrWaitAdvisor.advise(bestCop, history);

            groups.add(new ProductComparisonGroup(
                    entry.getKey(),
                    best.title(),
                    best.brand() == null ? "Sin marca especificada" : best.brand(),
                    best.gtin(),
                    best.imageUrl(),
                    exact,
                    badge,
                    compared,
                    best,
                    advice
            ));
        }

        groups.sort(Comparator.comparing(g -> g.bestOffer().totalInTarget().getAmount()));
        return groups;
    }

    private ComparedOffer toComparedOffer(RawOffer raw, String targetCurrency) {
        Money base = new Money(raw.price(), raw.currency());
        Money shipping = (raw.shippingKnown() && raw.shippingCost() != null)
                ? new Money(raw.shippingCost(), raw.currency())
                : null;
        Money tax = (raw.taxKnown() && raw.taxAmount() != null)
                ? new Money(raw.taxAmount(), raw.currency())
                : null;

        PriceCalculator.PriceBreakdown breakdown = priceCalculator.calculateFinalPrice(
                base, shipping, raw.shippingKnown(), tax, raw.taxKnown());
        Money convertedTotal = priceCalculator.convert(breakdown.knownTotal(), targetCurrency, null);

        String label = breakdown.complete() ? "Total estimado" : "Menor precio publicado (costos pendientes)";
        String img = raw.imageUrls().isEmpty() ? "" : raw.imageUrls().getFirst();

        return new ComparedOffer(
                raw.storeCode() + ":" + raw.externalId(),
                raw.storeCode(),
                friendlyStoreName(raw.storeCode()),
                raw.title(),
                raw.brand(),
                raw.model(),
                raw.gtin(),
                raw.productUrl(),
                img,
                base,
                shipping,
                raw.shippingKnown(),
                tax,
                raw.taxKnown(),
                convertedTotal,
                breakdown.complete(),
                label,
                raw.dataKind(),
                raw.availability(),
                raw.fetchedAt(),
                raw.attributes()
        );
    }

    /**
     * Convierte el código interno de tienda en su nombre visible en español.
     *
     * @param code código de tienda
     * @return nombre comercial para mostrar al usuario
     */
    public String friendlyStoreName(String code) {
        return switch (code.toUpperCase()) {
            case "MERCADOLIBRE_CO" -> "Mercado Libre Colombia";
            case "AMAZON_CO" -> "Amazon (Envío a Colombia)";
            case "TEMU_CO" -> "Temu Colombia";
            case "FALABELLA_CO" -> "Falabella Colombia";
            case "ALIEXPRESS_CO" -> "AliExpress Choice";
            default -> code;
        };
    }

    private record StoreResult(List<RawOffer> offers, StoreQueryStatus status) {
    }
}
