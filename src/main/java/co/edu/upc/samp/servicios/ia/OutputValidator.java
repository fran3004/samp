package co.edu.upc.samp.servicios.ia;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida la respuesta generada por el proveedor de IA contra la evidencia real
 * obtenida por las herramientas de solo lectura antes de mostrarla al usuario.
 */
@Component
public class OutputValidator {

    /**
     * Resultado de validación de salida de IA (corresponde a {@code ai_validation_results}).
     *
     * @param passed {@code true} si la respuesta cumple las reglas de transparencia
     * @param issues lista de observaciones encontradas
     */
    public record ValidationResult(boolean passed, List<String> issues) {
    }

    /**
     * Comprueba que la respuesta no esté vacía y no prometa compras ni pagos dentro de SAMP.
     *
     * @param answer   texto generado por el proveedor
     * @param evidence evidencia de herramientas utilizada
     * @return resultado de validación
     */
    public ValidationResult validate(String answer, List<AiTool.ToolResult> evidence) {
        List<String> issues = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            issues.add("La respuesta del modelo llegó vacía.");
        } else {
            String lower = answer.toLowerCase();
            if (lower.contains("paga aquí") || lower.contains("procesar tu pago")) {
                issues.add("SAMP no procesa pagos ni checkout; debe redirigir a la tienda externa.");
            }
        }
        return new ValidationResult(issues.isEmpty(), issues);
    }
}
