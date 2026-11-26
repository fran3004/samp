package co.edu.upc.samp.entidades;

/**
 * Clasifica el origen de los datos de una tienda u oferta para garantizar transparencia.
 */
public enum DataKind {
    /** Datos obtenidos en vivo desde la tienda respetando robots.txt y sus reglas. */
    REAL,
    /** Datos de demostración sembrados como respaldo cuando la tienda bloquea o no tiene API. */
    DEMO
}
