package co.edu.upc.samp.entidades;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Objeto de valor inmutable que encapsula una oferta cruda capturada por un conector
 * antes de su validación, normalización y emparejamiento en el catálogo canónico.
 */
public record RawOffer(
        String storeCode,
        String externalId,
        String title,
        String productUrl,
        BigDecimal price,
        String currency,
        BigDecimal shippingCost,
        boolean shippingKnown,
        BigDecimal taxAmount,
        boolean taxKnown,
        String availability,
        String brand,
        String model,
        String gtin,
        DataKind dataKind,
        List<String> imageUrls,
        Map<String, String> attributes,
        Instant fetchedAt
) {
    /**
     * Constructor compacto que garantiza colecciones inmutables y valores por defecto seguros.
     */
    public RawOffer {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(attributes);
        fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
        availability = (availability == null || availability.isBlank()) ? "UNKNOWN" : availability;
        dataKind = dataKind == null ? DataKind.DEMO : dataKind;
    }
}
