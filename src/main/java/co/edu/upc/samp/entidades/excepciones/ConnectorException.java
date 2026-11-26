package co.edu.upc.samp.entidades.excepciones;

/**
 * Excepción lanzada cuando un conector de tienda falla por bloqueo, red o violación de robots.txt.
 */
public class ConnectorException extends SampException {

    private final String storeCode;

    /**
     * Construye una excepción de conector especificando la tienda afectada.
     *
     * @param storeCode código de la tienda
     * @param message   motivo del fallo
     * @param cause     causa técnica opcional
     */
    public ConnectorException(String storeCode, String message, Throwable cause) {
        super(
                "Error al consultar la tienda " + storeCode + ": " + message,
                "El sistema usará el respaldo de demostración o los datos más recientes disponibles.",
                cause
        );
        this.storeCode = storeCode;
    }

    public String getStoreCode() {
        return storeCode;
    }
}
