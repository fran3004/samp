package co.edu.upc.samp.entidades.excepciones;

/**
 * Excepción lanzada cuando una entidad solicitada no existe en la fuente de datos.
 */
public class EntityNotFoundException extends SampException {

    /**
     * Construye la excepción indicando la entidad y el identificador no encontrado.
     *
     * @param entityName nombre legible de la entidad
     * @param identifier identificador consultado
     */
    public EntityNotFoundException(String entityName, Object identifier) {
        super(
                "No se encontró " + entityName + " con identificador: " + identifier,
                "Verifica el identificador e inténtalo nuevamente."
        );
    }
}
