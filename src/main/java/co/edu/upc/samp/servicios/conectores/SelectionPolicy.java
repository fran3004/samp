package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.StoreConnectorConfig;

import java.util.Comparator;
import java.util.List;

/**
 * Estrategia polimórfica de ordenamiento y selección de conectores disponibles para una tienda.
 */
public interface SelectionPolicy {

    /**
     * Ordena los conectores disponibles según los criterios de la política.
     *
     * @param connectors configuraciones de conectores de la tienda
     * @return lista filtrada y priorizada
     */
    List<StoreConnectorConfig> rank(List<StoreConnectorConfig> connectors);
}
