package co.edu.upc.samp.entidades.excepciones;

/**
 * Excepción base de dominio para errores controlados de negocio en SAMP.
 * Permite propagar mensajes claros al usuario final sin exponer detalles técnicos.
 */
public class SampException extends RuntimeException {

    private final String userHint;

    /**
     * Crea una excepción de negocio con mensaje y sugerencia de acción para el usuario.
     *
     * @param message  descripción clara de lo ocurrido
     * @param userHint recomendación práctica de cómo continuar
     */
    public SampException(String message, String userHint) {
        super(message);
        this.userHint = userHint;
    }

    /**
     * Crea una excepción de negocio encadenando una causa técnica subyacente.
     *
     * @param message  descripción clara de lo ocurrido
     * @param userHint recomendación práctica de cómo continuar
     * @param cause    causa raíz capturada
     */
    public SampException(String message, String userHint, Throwable cause) {
        super(message, cause);
        this.userHint = userHint;
    }

    public String getUserHint() {
        return userHint;
    }
}
