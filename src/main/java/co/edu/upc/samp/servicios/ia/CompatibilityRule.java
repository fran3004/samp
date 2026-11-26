package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.entidades.RuleSeverity;
import co.edu.upc.samp.servicios.SearchService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regla polimórfica de validación para configuraciones armadas en cualquier categoría
 * (presupuesto, envíos desconocidos y compatibilidad técnica como hardware/PC).
 */
public interface CompatibilityRule {

    /**
     * Código único de la regla (coincide con {@code compat_rules.code}).
     *
     * @return código de regla
     */
    String code();

    /**
     * Severidad de la regla ({@code ERROR} o {@code WARNING}).
     *
     * @return severidad
     */
    RuleSeverity severity();

    /**
     * Evalúa la configuración propuesta contra esta regla.
     *
     * @param proposal configuración con presupuesto, categoría y ofertas seleccionadas
     * @return resultado de verificación con explicación en español
     */
    RuleEvaluation check(BuildProposal proposal);

    /**
     * Configuración propuesta por el usuario o por el asistente de IA.
     *
     * @param category  categoría libre (GENERAL, PC, HOGAR, ESTUDIO, etc.)
     * @param budgetCop presupuesto máximo en COP (opcional)
     * @param items     ofertas seleccionadas para la lista armada
     */
    record BuildProposal(String category, BigDecimal budgetCop, List<SearchService.ComparedOffer> items) {
    }

    /**
     * Resultado individual de una regla de compatibilidad o presupuesto.
     *
     * @param code     código de regla
     * @param severity severidad
     * @param passed   {@code true} si se cumple la regla
     * @param message  mensaje claro en español para el usuario
     */
    record RuleEvaluation(String code, RuleSeverity severity, boolean passed, String message) {
    }
}
