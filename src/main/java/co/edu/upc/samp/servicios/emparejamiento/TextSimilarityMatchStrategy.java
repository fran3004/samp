package co.edu.upc.samp.servicios.emparejamiento;

import co.edu.upc.samp.entidades.RawOffer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Estrategia de nivel 3: similitud léxica por índice de Jaccard sobre tokens del título.
 * Nunca declara equivalencia exacta sin código verificable; clasifica el resultado como «similar».
 */
@Component
public class TextSimilarityMatchStrategy implements MatchStrategy {

    private static final double SIMILARITY_THRESHOLD = 0.45;

    @Override
    public int level() {
        return 3;
    }

    @Override
    public String methodName() {
        return "TEXT_SIMILARITY";
    }

    @Override
    public Optional<MatchDecision> evaluate(RawOffer reference, RawOffer candidate) {
        Set<String> tokensA = tokenize(reference.title());
        Set<String> tokensB = tokenize(candidate.title());
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return Optional.empty();
        }
        long intersection = tokensA.stream().filter(tokensB::contains).count();
        long union = tokensA.size() + tokensB.size() - intersection;
        double jaccard = union == 0 ? 0.0 : (double) intersection / union;
        if (jaccard >= SIMILARITY_THRESHOLD) {
            return Optional.of(new MatchDecision(
                    false,
                    level(),
                    Math.min(0.85, jaccard),
                    methodName(),
                    "Producto similar por coincidencia de título (sin código GTIN compartido)"
            ));
        }
        return Optional.empty();
    }

    private Set<String> tokenize(String title) {
        if (title == null) {
            return Set.of();
        }
        return Arrays.stream(title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9áéíóúñ\\s]", " ").split("\\s+"))
                .filter(t -> t.length() >= 3)
                .collect(Collectors.toSet());
    }
}
