package co.edu.upc.samp.servicios.ia;

import java.util.Map;

/**
 * Contrato de herramienta de solo lectura ejecutable por el asistente de IA de SAMP.
 * Todas las 10 herramientas del sistema implementan esta interfaz y nunca mutan la base de datos.
 */
public interface AiTool {

    /**
     * Nombre oficial de la herramienta registrada en el catálogo de herramientas.
     *
     * @return nombre único de la herramienta
     */
    String name();

    /**
     * Descripción breve en español del propósito de la herramienta.
     *
     * @return descripción legible
     */
    String description();

    /**
     * Ejecuta la consulta de solo lectura con los argumentos provistos.
     *
     * @param arguments mapa de parámetros clave-valor
     * @return resultado estructurado y evidencia en texto para el asistente
     */
    ToolResult execute(Map<String, Object> arguments);

    /**
     * Resultado inmutable de la ejecución de una herramienta de IA.
     *
     * @param toolName nombre de la herramienta invocada
     * @param success  indica si se obtuvieron datos válidos
     * @param summary  resumen verificable en español
     * @param data     datos estructurados adicionales
     */
    record ToolResult(String toolName, boolean success, String summary, Object data) {
    }
}
