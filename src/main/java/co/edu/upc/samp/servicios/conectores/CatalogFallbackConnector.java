package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.ConnectorKind;
import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Conector de respaldo transparente ({@code DEMO}) para las 5 tiendas objetivo de Colombia.
 *
 * <p>Se activa automáticamente cuando el conector de scraping de una tienda es bloqueado
 * o restringido por {@code robots.txt}. Todas las ofertas emitidas llevan {@link DataKind#DEMO}
 * y distinguen explícitamente envíos conocidos y desconocidos.
 */
@Component
public class CatalogFallbackConnector extends AbstractStoreConnector {

    private final List<RawOffer> demoCatalog;

    /**
     * Inicializa el catálogo de demostración con productos representativos en las 5 tiendas.
     */
    public CatalogFallbackConnector() {
        Instant now = Instant.now();
        this.demoCatalog = List.of(
                // Sony WH-1000XM5 (GTIN verificable compartido en 4 tiendas)
                new RawOffer("MERCADOLIBRE_CO", "MCO-XM5-01", "Audífonos Sony WH-1000XM5 Cancelación de Ruido Negro",
                        "https://www.mercadolibre.com.co/p/MCO19528491", new BigDecimal("1349900"), "COP",
                        BigDecimal.ZERO, true, BigDecimal.ZERO, true, "IN_STOCK", "Sony", "WH-1000XM5", "0027242923232",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=600"),
                        Map.of("entrega", "24-48 h", "color", "Negro", "categoria", "Audio"), now),
                new RawOffer("AMAZON_CO", "B09XS7JWHH", "Sony WH-1000XM5 Wireless Noise Canceling Headphones",
                        "https://www.amazon.com/dp/B09XS7JWHH", new BigDecimal("298.00"), "USD",
                        BigDecimal.ZERO, true, new BigDecimal("56.62"), true, "IN_STOCK", "Sony", "WH-1000XM5", "0027242923232",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=600"),
                        Map.of("entrega", "5-10 días", "impuestos", "Depósito de importación calculado", "categoria", "Audio"), now),
                new RawOffer("FALABELLA_CO", "FAL-XM5-01", "Audífonos Bluetooth Sony WH-1000XM5 Negro",
                        "https://www.falabella.com.co/falabella-co/product/xm5", new BigDecimal("1499900"), "COP",
                        null, false, BigDecimal.ZERO, true, "IN_STOCK", "Sony", "WH-1000XM5", "0027242923232",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=600"),
                        Map.of("retiro", "Gratis en tienda", "categoria", "Audio"), now),

                // Mouse Logitech G502 HERO (comparación en 5 tiendas)
                new RawOffer("MERCADOLIBRE_CO", "MCO-G502-01", "Mouse Gamer Logitech G502 HERO 25K DPI",
                        "https://www.mercadolibre.com.co/p/MCO15192341", new BigDecimal("199900"), "COP",
                        BigDecimal.ZERO, true, BigDecimal.ZERO, true, "IN_STOCK", "Logitech", "G502 HERO", "0097855141224",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1527814050087-3793815479db?w=600"),
                        Map.of("dpi", "25600", "categoria", "Periféricos"), now),
                new RawOffer("AMAZON_CO", "B07GBZ4Q68", "Logitech G502 HERO High Performance Wired Gaming Mouse",
                        "https://www.amazon.com/dp/B07GBZ4Q68", new BigDecimal("39.99"), "USD",
                        BigDecimal.ZERO, true, BigDecimal.ZERO, true, "IN_STOCK", "Logitech", "G502 HERO", "0097855141224",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1527814050087-3793815479db?w=600"),
                        Map.of("dpi", "25600", "envio", "Gratis a Colombia >$35 USD", "categoria", "Periféricos"), now),
                new RawOffer("ALIEXPRESS_CO", "ALI-G502-01", "Logitech G502 HERO Gaming Mouse Choice",
                        "https://es.aliexpress.com/item/100500123456.html", new BigDecimal("36.50"), "USD",
                        null, false, null, false, "IN_STOCK", "Logitech", "G502 HERO", "0097855141224",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1527814050087-3793815479db?w=600"),
                        Map.of("modalidad", "Choice 10-15 días", "categoria", "Periféricos"), now),

                // Freidora de Aire Ninja 5.2L (Hogar / Falabella vs ML vs Temu)
                new RawOffer("FALABELLA_CO", "FAL-AIR-01", "Freidora de Aire Ninja AF161 Max XL 5.2 Litros",
                        "https://www.falabella.com.co/falabella-co/product/ninja-af161", new BigDecimal("549900"), "COP",
                        BigDecimal.ZERO, true, BigDecimal.ZERO, true, "IN_STOCK", "Ninja", "AF161", "0622356563093",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1585515320310-259814833e62?w=600"),
                        Map.of("capacidad", "5.2L", "categoria", "Hogar"), now),
                new RawOffer("MERCADOLIBRE_CO", "MCO-AIR-01", "Freidora de Aire Ninja Max XL 5.2L AF161",
                        "https://www.mercadolibre.com.co/p/MCO18234111", new BigDecimal("579000"), "COP",
                        BigDecimal.ZERO, true, BigDecimal.ZERO, true, "IN_STOCK", "Ninja", "AF161", "0622356563093",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1585515320310-259814833e62?w=600"),
                        Map.of("capacidad", "5.2L", "categoria", "Hogar"), now),

                // Accesorios económicos Temu y AliExpress
                new RawOffer("TEMU_CO", "TEMU-HUB-01", "Hub USB-C 7 en 1 HDMI 4K Aluminio para Portátil",
                        "https://www.temu.com/co/goods-hub-usbc.html", new BigDecimal("42900"), "COP",
                        BigDecimal.ZERO, true, BigDecimal.ZERO, true, "IN_STOCK", "Genérico", "HUB-7IN1", "6901234567890",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1625842268584-8f3296236761?w=600"),
                        Map.of("envio", "Estándar gratis 1-2 semanas", "categoria", "Accesorios"), now),
                new RawOffer("ALIEXPRESS_CO", "ALI-HUB-01", "Ugreen Hub USB-C 7 en 1 HDMI 4K Choice",
                        "https://es.aliexpress.com/item/100500999111.html", new BigDecimal("14.90"), "USD",
                        BigDecimal.ZERO, true, null, false, "IN_STOCK", "Ugreen", "CM478", "6901234567890",
                        DataKind.DEMO, List.of("https://images.unsplash.com/photo-1625842268584-8f3296236761?w=600"),
                        Map.of("modalidad", "Choice", "categoria", "Accesorios"), now),

                // Nutella GTIN real de referencia (3017620422003)
                new RawOffer("MERCADOLIBRE_CO", "MCO-NUT-01", "Crema de Avellanas Nutella 350g",
                        "https://www.mercadolibre.com.co/p/MCO19901122", new BigDecimal("26900"), "COP",
                        new BigDecimal("8500"), true, BigDecimal.ZERO, true, "IN_STOCK", "Ferrero", "Nutella 350g", "3017620422003",
                        DataKind.DEMO, List.of("https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.633.400.jpg"),
                        Map.of("peso", "350g", "categoria", "Alimentos"), now)
        );
    }

    @Override
    public String storeCode() {
        return "ALL_DEMO_FALLBACK";
    }

    @Override
    public ConnectorKind kind() {
        return ConnectorKind.FALLBACK;
    }

    /**
     * Filtra el catálogo de respaldo para una tienda específica y una consulta.
     *
     * @param storeCode código de tienda objetivo
     * @param query     consulta de búsqueda
     * @return ofertas demo de esa tienda
     */
    public List<RawOffer> searchForStore(String storeCode, SearchQuery query) {
        return executeSearch(query).stream()
                .filter(o -> o.storeCode().equalsIgnoreCase(storeCode))
                .toList();
    }

    @Override
    protected List<RawOffer> executeSearch(SearchQuery query) {
        if (query.hasGtin()) {
            return demoCatalog.stream()
                    .filter(o -> query.gtin().equalsIgnoreCase(o.gtin()))
                    .toList();
        }
        String q = query.text().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return demoCatalog;
        }
        String[] tokens = q.split("\\s+");
        return demoCatalog.stream()
                .filter(o -> {
                    String haystack = (o.title() + " " + (o.brand() != null ? o.brand() : "") + " " + o.storeCode())
                            .toLowerCase(Locale.ROOT);
                    for (String token : tokens) {
                        if (haystack.contains(token)) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }
}
