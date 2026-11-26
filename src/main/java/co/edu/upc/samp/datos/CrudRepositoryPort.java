package co.edu.upc.samp.datos;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistencia CRUD para la capa de datos de SAMP.
 *
 * <p>Permite intercambiar implementaciones (base de datos PostgreSQL/Supabase o archivos
 * JSON con {@code ArrayList}) sin modificar la capa de servicios.
 *
 * @param <T>  tipo de entidad
 * @param <ID> tipo de identificador único
 */
public interface CrudRepositoryPort<T, ID> {

    /**
     * Crea y persiste una nueva entidad verificando que no existan duplicados.
     *
     * @param entity entidad a registrar
     * @return entidad persistida con su identificador asignado
     */
    T save(T entity);

    /**
     * Consulta una entidad por su identificador único.
     *
     * @param id identificador a buscar
     * @return entidad encontrada o vacío si no existe
     */
    Optional<T> findById(ID id);

    /**
     * Lista todas las entidades almacenadas.
     *
     * @return lista de entidades
     */
    List<T> findAll();

    /**
     * Actualiza una entidad existente manteniendo la consistencia de datos.
     *
     * @param id     identificador de la entidad
     * @param entity nuevos datos validados
     * @return entidad actualizada
     */
    T update(ID id, T entity);

    /**
     * Elimina una entidad por su identificador.
     *
     * @param id identificador a eliminar
     */
    void deleteById(ID id);
}
