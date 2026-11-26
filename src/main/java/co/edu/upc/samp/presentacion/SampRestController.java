package co.edu.upc.samp.presentacion;

import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.servicios.SearchService;
import co.edu.upc.samp.servicios.ia.AiOrchestrator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador REST de SAMP documentado con OpenAPI para búsquedas en JSON,
 * consultas interactivas al asistente de IA general y verificación de estado.
 */
@RestController
@RequestMapping("/api")
public class SampRestController {

    private final SearchService searchService;
    private final AiOrchestrator aiOrchestrator;

    /**
     * Construye el controlador REST inyectando únicamente servicios de negocio.
     *
     * @param searchService  servicio de búsqueda paralela
     * @param aiOrchestrator orquestador del asistente de IA general
     */
    public SampRestController(SearchService searchService, AiOrchestrator aiOrchestrator) {
        this.searchService = searchService;
        this.aiOrchestrator = aiOrchestrator;
    }

    /**
     * Cuerpo de solicitud para conversar o armar una lista con el asistente de IA.
     *
     * @param message   mensaje del comprador
     * @param mode      {@code CHAT} o {@code BUILD}
     * @param budgetCop presupuesto opcional en COP
     * @param category  categoría libre opcional
     */
    public record AiChatRequest(String message, String mode, Long budgetCop, String category) {
    }

    /**
     * Ejecuta una búsqueda comparativa sobre las 5 tiendas de Colombia.
     *
     * @param q        texto libre
     * @param gtin     código GTIN opcional
     * @param currency moneda de visualización ({@code COP} o {@code USD})
     * @param live     habilita consulta en vivo respetando {@code robots.txt}
     * @return respuesta completa con grupos y estado por tienda
     */
    @GetMapping("/search")
    public SearchService.SearchResponse search(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "gtin", defaultValue = "") String gtin,
            @RequestParam(name = "currency", defaultValue = "COP") String currency,
            @RequestParam(name = "live", defaultValue = "false") boolean live
    ) {
        return searchService.search(new SearchQuery(q, gtin, "", currency, 20), live);
    }

    /**
     * Procesa una pregunta o solicitud de armado en el asistente de IA general.
     *
     * @param body datos de la consulta
     * @return respuesta trazable con herramientas invocadas y validación
     */
    @PostMapping("/ai/chat")
    public AiOrchestrator.ChatReply chat(@RequestBody AiChatRequest body) {
        String msg = body == null ? "" : body.message();
        String mode = (body == null || body.mode() == null) ? "CHAT" : body.mode();
        Long budget = body == null ? null : body.budgetCop();
        String category = body == null ? "GENERAL" : body.category();
        return aiOrchestrator.ask(msg, mode, budget, category);
    }

    /**
     * Endpoint ligero de salud para monitoreo en Render y pruebas automatizadas.
     *
     * @return mapa de estado actual
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "app", "SAMP - Sistema Automatizado de Mejores Precios",
                "timestamp", Instant.now().toString()
        );
    }
}
