package co.edu.upc.samp.entidades.excepciones;

/**
 * Excepción lanzada cuando los datos ingresados por el usuario incumplen reglas de validación.
 */
public class ValidationException extends SampException {

    /**
     * Construye una excepción de validación con mensaje claro para el usuario.
     *
     * @param message detalle del campo inválido
     */
    public ValidationException(String message) {
        super(message, "Corrige los campos señalados antes de guardar.");
    }
}
