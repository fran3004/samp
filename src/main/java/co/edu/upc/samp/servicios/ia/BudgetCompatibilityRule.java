package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.entidades.RuleSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Regla general que verifica si el total estimado de la configuración respeta el presupuesto del usuario.
 */
@Component
public class BudgetCompatibilityRule implements CompatibilityRule {

    @Override
    public String code() {
        return "BUDGET_LIMIT";
    }

    @Override
    public RuleSeverity severity() {
        return RuleSeverity.WARNING;
    }

    @Override
    public RuleEvaluation check(BuildProposal proposal) {
        if (proposal.budgetCop() == null || proposal.budgetCop().compareTo(BigDecimal.ZERO) <= 0) {
            return new RuleEvaluation(code(), severity(), true, "Sin límite de presupuesto definido.");
        }
        BigDecimal total = proposal.items().stream()
                .map(i -> i.totalInTarget().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean ok = total.compareTo(proposal.budgetCop()) <= 0;
        String msg = ok
                ? String.format("El total estimado ($%,.0f COP) está dentro de tu presupuesto ($%,.0f COP).", total, proposal.budgetCop())
                : String.format("El total estimado ($%,.0f COP) supera tu presupuesto ($%,.0f COP).", total, proposal.budgetCop());
        return new RuleEvaluation(code(), severity(), ok, msg);
    }
}
