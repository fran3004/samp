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
     * Solicitud inmutable enviada a un proveedor de IA.
     *
     * @param userMessage  pregunta o requerimiento del comprador
     * @param mode         modo de conversación ({@code CHAT} o {@code BUILD})
     * @param budgetCop    presupuesto opcional en COP
     * @param category     categoría libre opcional para armar listas/setups
     * @param toolEvidence evidencia verificada de solo lectura proveniente del catálogo
     */
    record AiRequest(
            String userMessage,
            String mode,
            Long budgetCop,
            String category,
            List<AiTool.ToolResult> toolEvidence
    ) {
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
