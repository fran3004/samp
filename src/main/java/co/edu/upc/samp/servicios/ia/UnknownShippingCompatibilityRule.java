package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.entidades.RuleSeverity;
import org.springframework.stereotype.Component;

/**
 * Regla general de transparencia: advierte cuando alguno de los productos armados
 * tiene envío o impuestos desconocidos, indicando que el total estimado es incompleto.
 */
@Component
public class UnknownShippingCompatibilityRule implements CompatibilityRule {

    @Override
    public String code() {
        return "UNKNOWN_SHIPPING";
    }

    @Override
    public RuleSeverity severity() {
        return RuleSeverity.WARNING;
    }

    @Override
    public RuleEvaluation check(BuildProposal proposal) {
        long incompleteCount = proposal.items().stream()
                .filter(i -> !i.completeTotal())
                .count();
        if (incompleteCount == 0) {
            return new RuleEvaluation(code(), severity(), true,
                    "Todos los artículos incluidos tienen envío e impuestos conocidos.");
        }
        return new RuleEvaluation(code(), severity(), false,
                incompleteCount + " artículo(s) tienen envío o impuestos desconocidos; el total estimado es incompleto.");
    }
}
