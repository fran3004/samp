package co.edu.upc.samp.servicios.emparejamiento;

import co.edu.upc.samp.entidades.RawOffer;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Estrategia de nivel 1: emparejamiento exacto y verificable por código universal GTIN/EAN/UPC.
 */
@Component
public class GtinMatchStrategy implements MatchStrategy {

    @Override
    public int level() {
        return 1;
    }

    @Override
    public String methodName() {
        return "GTIN_EXACT";
    }

    @Override
    public Optional<MatchDecision> evaluate(RawOffer reference, RawOffer candidate) {
        if (reference.gtin() == null || reference.gtin().isBlank()
                || candidate.gtin() == null || candidate.gtin().isBlank()) {
            return Optional.empty();
        }
        String gtinA = normalizeCode(reference.gtin());
        String gtinB = normalizeCode(candidate.gtin());
        if (gtinA.equals(gtinB)) {
            return Optional.of(new MatchDecision(
                    true,
                    level(),
                    1.0,
                    methodName(),
                    "Mismo producto verificado por código GTIN " + gtinA
            ));
        }
        return Optional.empty();
    }

    private String normalizeCode(String code) {
        return code.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }
}
