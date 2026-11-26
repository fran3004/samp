package co.edu.upc.samp.datos;

import co.edu.upc.samp.entidades.Store;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para tiendas monitoreadas por SAMP.
 */
public interface StoreRepositoryPort extends CrudRepositoryPort<Store, Long> {

    /**
     * Busca una tienda por su código único (por ejemplo, {@code MERCADOLIBRE_CO}).
     *
     * @param code código de la tienda
     * @return tienda encontrada o vacío
     */
    Optional<Store> findByCode(String code);

    /**
     * Retorna únicamente las tiendas activas.
     *
     * @return lista de tiendas habilitadas
     */
    List<Store> findAllActive();
}
