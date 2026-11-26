package co.edu.upc.samp.servicios.ia;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Proveedor de IA local y determinístico ({@code mock}) para desarrollo, pruebas y respaldo
 * cuando no hay clave o conectividad con Gemini. Maneja saludos, despedidas, preguntas fuera
 * de tema y respuestas fundamentadas en la evidencia del catálogo.
 */
@Component
public class MockAiProvider implements AiProvider {

    /**
     * Mensaje predeterminado cuando la consulta no tiene relación con productos ni compras en SAMP.
     */
    public static final String OFF_TOPIC_REPLY =
            "Soy el asistente de compras de SAMP y estoy aquí esperando tu pregunta relacionada con productos, "
                    + "comparación de precios en tiendas de Colombia o para ayudarte a armar una lista con tu presupuesto. "
                    + "¿Qué producto te gustaría consultar hoy?";

    @Override
    public String id() {
        return "mock";
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long start = System.currentTimeMillis();
        String answerText = switch (request.intent()) {
            case "GREETING" -> "¡Hola! Qué gusto saludarte. Soy el asistente de compras de SAMP. "
                    + "Puedes preguntarme por precios de productos en las 5 tiendas de Colombia, si conviene comprar o esperar, "
                    + "o pedirme que te arme una lista o setup según tu presupuesto. ¿Qué estás buscando hoy?";
            case "FAREWELL" -> "¡Hasta luego! Que tengas un excelente día. "
                    + "Cuando vuelvas a necesitar comparar precios o armar una compra inteligente, aquí estaré.";
            case "THANKS" -> "¡Con mucho gusto! Si quieres revisar otro producto, comparar tiendas o ajustar un presupuesto, solo escríbeme.";
            case "OFF_TOPIC" -> OFF_TOPIC_REPLY;
            default -> buildCatalogAnswer(request);
        };

        long elapsed = Math.max(1L, System.currentTimeMillis() - start);
        int tokens = Math.max(25, answerText.length() / 4);
        return new AiResponse(id(), "samp-mock-v1", answerText, tokens, elapsed);
    }

    private String buildCatalogAnswer(AiRequest request) {
        StringBuilder answer = new StringBuilder();
        if ("BUILD".equalsIgnoreCase(request.mode())) {
            String categoryLabel = (request.category() == null || request.category().isBlank())
                    ? "configuración personalizada"
                    : request.category();
            answer.append("Propuesta armada por SAMP para tu ").append(categoryLabel);
            if (request.budgetCop() != null && request.budgetCop() > 0) {
                answer.append(" con presupuesto objetivo de $")
                        .append(String.format("%,d", request.budgetCop())).append(" COP");
            }
            answer.append(":\n\n");
        } else {
            answer.append("Esto es lo que encontré en el catálogo actual de SAMP:\n\n");
        }

        if (request.toolEvidence() == null || request.toolEvidence().isEmpty()) {
            answer.append("No encontré productos coincidentes en las 5 tiendas para «")
                    .append(request.userMessage())
                    .append("». Intenta con un nombre más general, una marca o un código GTIN.");
        } else {
            String summary = request.toolEvidence().stream()
                    .filter(AiTool.ToolResult::success)
                    .map(r -> "- " + r.summary())
                    .collect(Collectors.joining("\n"));
            answer.append(summary);
            answer.append("\n\nNota de transparencia: los valores marcados como DEMO corresponden al catálogo de respaldo y los costos de envío o impuestos desconocidos nunca se asumen como cero.");
        }
        return answer.toString();
    }

    @Override
    public boolean supportsVision() {
        return false;
    }
}
