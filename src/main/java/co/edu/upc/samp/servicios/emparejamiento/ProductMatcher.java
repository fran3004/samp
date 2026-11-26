package co.edu.upc.samp.servicios.emparejamiento;

import co.edu.upc.samp.entidades.RawOffer;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Motor de emparejamiento de productos que aplica las estrategias {@link MatchStrategy}
 * en orden estricto de nivel (GTIN -> marca/modelo -> similitud textual).
 */
@Service
public class ProductMatcher {

    private final List<MatchStrategy> orderedStrategies;

    /**
     * Construye el motor ordenando automáticamente todas las estrategias inyectadas por nivel.
     *
     * @param strategies estrategias disponibles en el contexto
     */
    public ProductMatcher(List<MatchStrategy> strategies) {
        this.orderedStrategies = strategies.stream()
                .sorted(Comparator.comparingInt(MatchStrategy::level))
                .toList();
    }

    /**
     * Evalúa dos ofertas aplicando polimórficamente las estrategias en orden de prioridad.
     *
     * @param reference oferta base
     * @param candidate oferta a comparar
     * @return primera coincidencia encontrada o vacío si no hay relación
     */
    public Optional<MatchStrategy.MatchDecision> match(RawOffer reference, RawOffer candidate) {
        for (MatchStrategy strategy : orderedStrategies) {
            Optional<MatchStrategy.MatchDecision> decision = strategy.evaluate(reference, candidate);
            if (decision.isPresent()) {
                return decision;
            }
        }
        return Optional.empty();
    }
}
