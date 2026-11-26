package co.edu.upc.samp.entidades.excepciones;

/**
 * Excepción lanzada cuando se intenta registrar un identificador o clave única ya existente.
 */
public class DuplicateEntityException extends SampException {

    /**
     * Construye la excepción detallando el campo duplicado.
     *
     * @param entityName nombre legible de la entidad
     * @param field      campo único en conflicto
     * @param value      valor repetido
     */
    public DuplicateEntityException(String entityName, String field, Object value) {
        super(
                "Ya existe un registro de " + entityName + " con " + field + " = " + value,
                "Usa un valor diferente o actualiza el registro existente."
        );
    }
}
