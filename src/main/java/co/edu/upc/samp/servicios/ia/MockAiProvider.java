package co.edu.upc.samp.servicios.ia;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Proveedor de IA local y determinístico ({@code mock}) para desarrollo, pruebas y demostraciones
 * sin requerir clave de API externa. Fundamenta todas sus respuestas en la evidencia de las herramientas.
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String id() {
        return "mock";
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long start = System.currentTimeMillis();
        StringBuilder answer = new StringBuilder();

        if ("BUILD".equalsIgnoreCase(request.mode())) {
            String categoryLabel = (request.category() == null || request.category().isBlank())
                    ? "configuración personalizada"
                    : request.category();
            answer.append("Propuesta armada por SAMP para tu ").append(categoryLabel);
            if (request.budgetCop() != null && request.budgetCop() > 0) {
                answer.append(" con presupuesto objetivo de $").append(String.format("%,d", request.budgetCop())).append(" COP");
            }
            answer.append(":\n\n");
        } else {
            answer.append("Resumen verificado con el catálogo actual de SAMP:\n\n");
        }

        if (request.toolEvidence() == null || request.toolEvidence().isEmpty()) {
            answer.append("No encontré productos coincidentes en las 5 tiendas para «")
                    .append(request.userMessage())
                    .append("». Intenta con un nombre más general, una marca o un código GTIN.");
        } else {
            String summary = request.toolEvidence().stream()
                    .filter(AiTool.ToolResult::success)
                    .map(r -> "- [" + r.toolName() + "] " + r.summary())
                    .collect(Collectors.joining("\n"));
            answer.append(summary);
            answer.append("\n\nNota de transparencia: los valores marcados como DEMO corresponden al catálogo de respaldo y los costos de envío o impuestos desconocidos nunca se asumen como cero.");
        }

        long elapsed = Math.max(1L, System.currentTimeMillis() - start);
        int tokens = Math.max(30, answer.length() / 4);
        return new AiResponse(id(), "samp-mock-v1", answer.toString(), tokens, elapsed);
    }

    @Override
    public boolean supportsVision() {
        return false;
    }
}
