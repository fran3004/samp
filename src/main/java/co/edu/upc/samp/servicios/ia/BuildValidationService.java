package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.entidades.RuleSeverity;
import co.edu.upc.samp.servicios.SearchService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio general de armado y validación de configuraciones en cualquier categoría.
 * Aplica polimórficamente todas las implementaciones de {@link CompatibilityRule}.
 */
@Service
public class BuildValidationService {

    private final List<CompatibilityRule> rules;

    /**
     * Crea el validador inyectando todas las reglas activas en el contexto de Spring.
     *
     * @param rules reglas de compatibilidad y presupuesto
     */
    public BuildValidationService(List<CompatibilityRule> rules) {
        this.rules = rules;
    }

    /**
     * Ejecuta todas las reglas de validación sobre una propuesta de armado.
     *
     * @param proposal configuración a validar
     * @return lista de resultados por regla
     */
    public List<CompatibilityRule.RuleEvaluation> validate(CompatibilityRule.BuildProposal proposal) {
        List<CompatibilityRule.RuleEvaluation> results = new ArrayList<>();
        for (CompatibilityRule rule : rules) {
            results.add(rule.check(proposal));
        }
        return results;
    }

    /**
     * Calcula el total acumulado conocido de una lista de ofertas seleccionadas.
     *
     * @param items ofertas de la configuración
     * @return suma total conocida en la moneda destino
     */
    public BigDecimal calculateTotal(List<SearchService.ComparedOffer> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(i -> i.totalInTarget().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Indica si la configuración pasó todas las reglas con severidad {@link RuleSeverity#ERROR}.
     *
     * @param evaluations resultados de validación
     * @return {@code true} si no existen errores bloqueantes
     */
    public boolean isValid(List<CompatibilityRule.RuleEvaluation> evaluations) {
        return evaluations.stream()
                .noneMatch(e -> !e.passed() && e.severity() == RuleSeverity.ERROR);
    }
}
