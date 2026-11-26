package co.edu.upc.samp.entidades;

/**
 * Estados posibles del disyuntor (circuit breaker) de un conector de tienda.
 */
public enum CircuitState {
    /** Operación normal. */
    CLOSED,
    /** Conector pausado tras fallos consecutivos. */
    OPEN,
    /** Conector en prueba de recuperación tras el tiempo de espera. */
    HALF_OPEN
}
