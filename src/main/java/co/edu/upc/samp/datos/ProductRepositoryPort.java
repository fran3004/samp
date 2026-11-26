package co.edu.upc.samp.datos;

import co.edu.upc.samp.entidades.Product;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para productos canónicos del catálogo.
 */
public interface ProductRepositoryPort extends CrudRepositoryPort<Product, Long> {

    /**
     * Busca productos cuyo nombre contenga el texto dado (sin distinguir mayúsculas).
     *
     * @param query texto de búsqueda
     * @return productos coincidentes
     */
    List<Product> searchByName(String query);

    /**
     * Busca un producto exacto por su código de barras GTIN/EAN.
     *
     * @param gtin código universal del producto
     * @return producto encontrado o vacío
     */
    Optional<Product> findByGtin(String gtin);
}
