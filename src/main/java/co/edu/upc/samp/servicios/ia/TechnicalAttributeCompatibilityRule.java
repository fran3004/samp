package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.entidades.RuleSeverity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regla técnica que verifica compatibilidad de atributos críticos (como {@code socket} o {@code memory_type})
 * cuando la configuración pertenece a categorías técnicas como PC o electrónica.
 */
@Component
public class TechnicalAttributeCompatibilityRule implements CompatibilityRule {

    @Override
    public String code() {
        return "CPU_MB_SOCKET";
    }

    @Override
    public RuleSeverity severity() {
        return RuleSeverity.ERROR;
    }

    @Override
    public RuleEvaluation check(BuildProposal proposal) {
        Set<String> sockets = proposal.items().stream()
                .map(i -> i.attributes().get("socket"))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (sockets.size() > 1) {
            return new RuleEvaluation(code(), severity(), false,
                    "Incompatibilidad detectada: se mezclaron componentes con sockets distintos " + sockets + ".");
        }
        return new RuleEvaluation(code(), severity(), true,
                "Sin conflictos técnicos detectados entre los atributos de los productos seleccionados.");
    }
}
