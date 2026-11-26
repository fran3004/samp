package co.edu.upc.samp.entidades;

/**
 * Objeto de valor inmutable que representa una consulta de búsqueda de productos
 * por texto libre o por identificador universal (GTIN/EAN).
 */
public record SearchQuery(
        String text,
        String gtin,
        String category,
        String targetCurrency,
        int limit
) {
    /**
     * Constructor compacto con saneamiento de parámetros.
     */
    public SearchQuery {
        text = text == null ? "" : text.trim();
        gtin = gtin == null ? "" : gtin.trim();
        category = category == null ? "" : category.trim();
        targetCurrency = (targetCurrency == null || targetCurrency.isBlank()) ? "COP" : targetCurrency.trim().toUpperCase();
        limit = limit <= 0 ? 20 : Math.min(limit, 100);
    }

    /**
     * Crea una consulta simple por texto en COP (sobrecarga de conveniencia).
     *
     * @param text texto buscado por el usuario
     * @return instancia de {@code SearchQuery}
     */
    public static SearchQuery byText(String text) {
        return new SearchQuery(text, "", "", "COP", 20);
    }

    /**
     * Crea una consulta exacta por código de barras GTIN/EAN.
     *
     * @param gtin código GTIN/EAN
     * @return instancia de {@code SearchQuery}
     */
    public static SearchQuery byGtin(String gtin) {
        return new SearchQuery("", gtin, "", "COP", 20);
    }

    public boolean hasGtin() {
        return !gtin.isBlank();
    }
}
