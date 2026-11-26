package co.edu.upc.samp.servicios.ia;

import java.util.List;

/**
 * Contrato de abstracción e intercambiabilidad para proveedores de IA en SAMP (RF-26).
 *
 * <p>Permite cambiar entre Gemini, MockProvider u otros modelos compatibles por configuración
 * sin modificar el orquestador ni las vistas.
 */
public interface AiProvider {

    /**
     * Identificador único del proveedor (por ejemplo, {@code mock} o {@code gemini}).
     *
     * @return identificador del proveedor
     */
    String id();

    /**
     * Genera una respuesta asistida a partir de la solicitud del usuario y la evidencia
     * recolectada por las herramientas de solo lectura del catálogo.
     *
     * @param request solicitud estructurada de conversación
     * @return respuesta generada con métricas de latencia y tokens
     */
    AiResponse chat(AiRequest request);

    /**
     * Indica si el proveedor soporta análisis de imágenes para búsqueda visual (RF-21).
     *
     * @return {@code true} si admite entradas visuales
     */
    boolean supportsVision();

    /**
     * Turno previo de la conversación para mantener continuidad natural en lenguaje humano.
     *
     * @param role rol del mensaje ({@code user} o {@code model})
     * @param text contenido del mensaje
     */
    record ConversationTurn(String role, String text) {
    }

    /**
     * Solicitud inmutable enviada a un proveedor de IA.
     *
     * @param userMessage  pregunta o requerimiento del comprador
     * @param mode         modo de conversación ({@code CHAT} o {@code BUILD})
     * @param intent       intención detectada ({@code GREETING}, {@code FAREWELL}, {@code THANKS}, {@code SHOPPING}, {@code BUILD}, {@code OFF_TOPIC})
     * @param budgetCop    presupuesto opcional en COP
     * @param category     categoría libre opcional para armar listas/setups
     * @param toolEvidence evidencia verificada de solo lectura proveniente del catálogo
     * @param history      turnos recientes de la conversación
     */
    record AiRequest(
            String userMessage,
            String mode,
            String intent,
            Long budgetCop,
            String category,
            List<AiTool.ToolResult> toolEvidence,
            List<ConversationTurn> history
    ) {
        /**
         * Constructor compacto que garantiza colecciones inmutables y valores por defecto seguros.
         */
        public AiRequest {
            intent = (intent == null || intent.isBlank()) ? "SHOPPING" : intent;
            toolEvidence = toolEvidence == null ? List.of() : List.copyOf(toolEvidence);
            history = history == null ? List.of() : List.copyOf(history);
        }

        /**
         * Sobrecarga de conveniencia para solicitudes de un solo turno sin historial previo.
         */
        public AiRequest(
                String userMessage,
                String mode,
                Long budgetCop,
                String category,
                List<AiTool.ToolResult> toolEvidence
        ) {
            this(userMessage, mode, "BUILD".equalsIgnoreCase(mode) ? "BUILD" : "SHOPPING",
                    budgetCop, category, toolEvidence, List.of());
        }
    }

    /**
     * Respuesta inmutable devuelta por un proveedor de IA.
     *
     * @param providerId      proveedor que respondió
     * @param model           modelo utilizado
     * @param answer          respuesta clara en español
     * @param estimatedTokens tokens aproximados consumidos
     * @param latencyMs       latencia medida en milisegundos
     */
    record AiResponse(
            String providerId,
            String model,
            String answer,
            int estimatedTokens,
            long latencyMs
    ) {
    }
}
