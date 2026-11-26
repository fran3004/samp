package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.ConnectorKind;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de abstracción para cualquier fuente de productos en SAMP
 * (scraping HTML permitido, API oficial, feed o respaldo de demostración).
 */
public interface StoreConnector {

    /**
     * Código único de la tienda atendida (por ejemplo, {@code MERCADOLIBRE_CO}).
     *
     * @return código de tienda
     */
    String storeCode();

    /**
     * Tipo de conector según su mecanismo de extracción.
     *
     * @return tipo de conector
     */
    ConnectorKind kind();

    /**
     * Busca ofertas en la tienda para la consulta suministrada.
     *
     * @param query consulta por texto o GTIN
     * @return lista de ofertas crudas validadas
     */
    List<RawOffer> search(SearchQuery query);

    /**
     * Busca una oferta específica por código de barras GTIN/EAN.
     *
     * @param gtin código universal del producto
     * @return oferta encontrada o vacío
     */
    Optional<RawOffer> fetchByGtin(String gtin);
}
