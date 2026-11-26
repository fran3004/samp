package co.edu.upc.samp.servicios.emparejamiento;

import co.edu.upc.samp.entidades.RawOffer;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Estrategia de nivel 2: emparejamiento verificable por marca y referencia/modelo exactos.
 */
@Component
public class BrandModelMatchStrategy implements MatchStrategy {

    @Override
    public int level() {
        return 2;
    }

    @Override
    public String methodName() {
        return "BRAND_MODEL_EXACT";
    }

    @Override
    public Optional<MatchDecision> evaluate(RawOffer reference, RawOffer candidate) {
        if (isBlank(reference.brand()) || isBlank(reference.model())
                || isBlank(candidate.brand()) || isBlank(candidate.model())) {
            return Optional.empty();
        }
        if (reference.brand().trim().equalsIgnoreCase(candidate.brand().trim())
                && reference.model().trim().equalsIgnoreCase(candidate.model().trim())) {
            return Optional.of(new MatchDecision(
                    true,
                    level(),
                    0.92,
                    methodName(),
                    "Mismo producto verificado por marca (" + reference.brand() + ") y modelo (" + reference.model() + ")"
            ));
        }
        return Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
