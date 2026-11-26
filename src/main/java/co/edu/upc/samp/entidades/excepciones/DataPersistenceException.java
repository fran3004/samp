package co.edu.upc.samp.entidades.excepciones;

/**
 * Excepción lanzada ante fallos de lectura o escritura en persistencia (archivos JSON o base de datos).
 */
public class DataPersistenceException extends SampException {

    /**
     * Construye una excepción de persistencia con mensaje claro y causa raíz.
     *
     * @param message descripción del error de lectura/escritura
     * @param cause   excepción de E/S o SQL subyacente
     */
    public DataPersistenceException(String message, Throwable cause) {
        super(
                message,
                "Verifica los permisos de escritura o la conexión de datos y vuelve a intentarlo.",
                cause
        );
    }
}
