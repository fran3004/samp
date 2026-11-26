package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.StoreConnectorConfig;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Política que prioriza conectores disponibles por menor {@code priority} y mayor {@code successRate}.
 */
@Component
public class SuccessRatePolicy implements SelectionPolicy {

    @Override
    public List<StoreConnectorConfig> rank(List<StoreConnectorConfig> connectors) {
        return connectors.stream()
                .filter(StoreConnectorConfig::isAvailable)
                .sorted(Comparator.comparingInt(StoreConnectorConfig::getPriority)
                        .thenComparing(StoreConnectorConfig::getSuccessRate, Comparator.reverseOrder()))
                .toList();
    }
}
