package co.edu.upc.samp.servicios.ia;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registro central de las 10 herramientas de solo lectura disponibles para el asistente de IA.
 */
@Component
public class ToolRegistry {

    private final Map<String, AiTool> toolsByName = new LinkedHashMap<>();

    /**
     * Registra automáticamente todas las implementaciones de {@link AiTool} presentes en el contexto.
     *
     * @param tools lista de herramientas inyectadas
     */
    public ToolRegistry(List<AiTool> tools) {
        for (AiTool tool : tools) {
            toolsByName.put(tool.name(), tool);
        }
    }

    /**
     * Obtiene una herramienta por su nombre exacto.
     *
     * @param name nombre de la herramienta
     * @return herramienta encontrada o vacío
     */
    public Optional<AiTool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    /**
     * Retorna el mapa inmutable de todas las herramientas registradas.
     *
     * @return mapa nombre -> herramienta
     */
    public Map<String, AiTool> allTools() {
        return Collections.unmodifiableMap(toolsByName);
    }
}
