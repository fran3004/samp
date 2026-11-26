package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.servicios.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Orquestador central del asistente de IA general de SAMP (chat conversacional + modo armar).
 *
 * <p>Ejecuta en orden: {@link AiGuardrails} -> clasificación de intención conversacional
 * (saludo, despedida, agradecimiento, compras/productos, armado o fuera de tema)
 * -> herramientas de solo lectura ({@link ToolRegistry}) cuando aplica
 * -> {@link AiProvider} configurado (Gemini con respaldo automático a {@link MockAiProvider})
 * -> {@link OutputValidator}.
 */
@Service
public class AiOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AiOrchestrator.class);

    private static final Pattern GREETING_PATTERN = Pattern.compile(
            "^(hola+|buenas?( tardes| noches| dias)?|buen dia|buenos dias|hey+|hello+|hi|saludos|que tal|que mas|como estas|como vas|quien eres|hola samp)[\\s!?.,]*$");
    private static final Pattern FAREWELL_PATTERN = Pattern.compile(
            "^(chao+|chau+|adios|hasta luego|hasta pronto|nos vemos|bye+|me voy|feliz dia|feliz tarde|feliz noche|hasta manana)[\\s!?.,]*$");
    private static final Pattern THANKS_PATTERN = Pattern.compile(
            "^(muchas )?gracias( por todo| totales)?|(te lo )?agradezco|perfecto gracias|ok gracias|vale gracias|excelente gracias[\\s!?.,]*$");

    private static final List<String> SHOPPING_KEYWORDS = List.of(
            "precio", "precios", "barato", "barata", "economico", "economica", "cuesta", "vale", "valor",
            "comprar", "compra", "esperar", "historial", "baja", "sube", "oferta", "ofertas", "descuento",
            "comparar", "compara", "mejor", "mejores", "recomienda", "recomendacion", "recomiendame",
            "tienda", "tiendas", "mercado libre", "mercadolibre", "amazon", "temu", "falabella", "aliexpress",
            "envio", "impuesto", "arancel", "garantia", "stock", "disponible", "alerta", "gtin", "codigo",
            "producto", "productos", "marca", "modelo", "catalogo", "especificaciones", "caracteristicas",
            "armar", "arma", "setup", "kit", "presupuesto", "combo", "lista", "regalo", "samp", "demo", "ayuda",
            "como funciona", "sony", "wh-1000xm5", "audifono", "audifonos", "auricular", "logitech", "g502",
            "mouse", "raton", "teclado", "monitor", "pantalla", "portatil", "laptop", "computador", "pc",
            "celular", "telefono", "smartphone", "tablet", "reloj", "smartwatch", "parlante", "camara",
            "consola", "playstation", "xbox", "nintendo", "ninja", "freidora", "licuadora", "cafetera",
            "nevera", "lavadora", "televisor", "tv", "hub", "usb", "cargador", "cable", "ssd", "ram",
            "grafica", "gpu", "procesador", "cpu", "silla", "escritorio", "microfono", "nutella",
            "tenis", "zapatos", "ropa", "mochila", "bolso", "perfume", "colchon", "hogar", "cocina", "gaming"
    );

    private final MockAiProvider mockProvider;
    private final GeminiAiProvider geminiProvider;
    private final ToolRegistry toolRegistry;
    private final AiGuardrails guardrails;
    private final OutputValidator outputValidator;
    private final String configuredProvider;

    /**
     * Construye el orquestador con ambos proveedores intercambiables y el registro de 10 herramientas.
     *
     * @param mockProvider       proveedor determinístico local
     * @param geminiProvider     proveedor Gemini vía API REST
     * @param toolRegistry       registro de las 10 herramientas de solo lectura
     * @param guardrails         filtro de seguridad y privacidad
     * @param outputValidator    validador de respuesta final
     * @param configuredProvider proveedor activo configurado en {@code samp.ai.provider}
     */
    public AiOrchestrator(
            MockAiProvider mockProvider,
            GeminiAiProvider geminiProvider,
            ToolRegistry toolRegistry,
            AiGuardrails guardrails,
            OutputValidator outputValidator,
            @Value("${samp.ai.provider:mock}") String configuredProvider
    ) {
        this.mockProvider = mockProvider;
        this.geminiProvider = geminiProvider;
        this.toolRegistry = toolRegistry;
        this.guardrails = guardrails;
        this.outputValidator = outputValidator;
        this.configuredProvider = configuredProvider;
    }

    /**
     * Respuesta enriquecida del orquestador hacia la capa de presentación.
     *
     * @param answer           texto final validado en español
     * @param providerUsed     proveedor que generó la respuesta ({@code gemini} o {@code mock})
     * @param modelUsed        modelo aplicado
     * @param toolsInvoked     nombres de las herramientas de solo lectura ejecutadas
     * @param validationPassed indica si superó {@link OutputValidator}
     * @param latencyMs        latencia total en milisegundos
     */
    public record ChatReply(
            String answer,
            String providerUsed,
            String modelUsed,
            List<String> toolsInvoked,
            boolean validationPassed,
            long latencyMs
    ) {
    }

    /**
     * Atiende una consulta libre o solicitud de armado sin historial previo.
     *
     * @param rawMessage pregunta o necesidad del usuario
     * @param mode       {@code CHAT} o {@code BUILD}
     * @param budgetCop  presupuesto máximo opcional en COP
     * @param category   categoría libre opcional
     * @return respuesta trazable con herramientas invocadas y validación
     */
    public ChatReply ask(String rawMessage, String mode, Long budgetCop, String category) {
        return ask(rawMessage, mode, budgetCop, category, List.of());
    }

    /**
     * Atiende una consulta libre sobre productos, conversación natural (saludos/despedidas)
     * o armado con presupuesto manteniendo el contexto reciente de la conversación.
     *
     * @param rawMessage pregunta o mensaje del usuario
     * @param mode       {@code CHAT} o {@code BUILD}
     * @param budgetCop  presupuesto máximo opcional en COP
     * @param category   categoría libre opcional
     * @param history    turnos recientes de conversación
     * @return respuesta trazable con herramientas invocadas y validación
     */
    public ChatReply ask(
            String rawMessage,
            String mode,
            Long budgetCop,
            String category,
            List<AiProvider.ConversationTurn> history
    ) {
        AiGuardrails.GuardResult guard = guardrails.screen(rawMessage);
        if (!guard.allowed()) {
            return new ChatReply(guard.reason(), "guardrails", "policy", List.of(), false, 1L);
        }

        String sanitized = guard.sanitized();
        String intent = classifyIntent(sanitized, mode, history);
        boolean buildMode = "BUILD".equals(intent);

        List<AiTool.ToolResult> evidence = ("SHOPPING".equals(intent) || buildMode)
                ? collectEvidence(sanitized, buildMode, budgetCop, category)
                : List.of();
        List<String> toolsInvoked = evidence.stream().map(AiTool.ToolResult::toolName).toList();

        AiProvider.AiRequest request = new AiProvider.AiRequest(
                sanitized,
                buildMode ? "BUILD" : "CHAT",
                intent,
                budgetCop,
                category,
                evidence,
                history
        );

        AiProvider.AiResponse response = invokeWithFallback(request);
        OutputValidator.ValidationResult validation = outputValidator.validate(response.answer(), evidence);

        String finalAnswer = validation.passed()
                ? response.answer()
                : mockProvider.chat(request).answer();

        return new ChatReply(
                finalAnswer,
                response.providerId(),
                response.model(),
                toolsInvoked,
                validation.passed(),
                response.latencyMs()
        );
    }

    private String classifyIntent(String message, String mode, List<AiProvider.ConversationTurn> history) {
        String normalized = normalizeAscii(message);

        if (GREETING_PATTERN.matcher(normalized).matches()) {
            return "GREETING";
        }
        if (FAREWELL_PATTERN.matcher(normalized).matches()) {
            return "FAREWELL";
        }
        if (THANKS_PATTERN.matcher(normalized).matches()) {
            return "THANKS";
        }
        if ("BUILD".equalsIgnoreCase(mode) || looksLikeBuildIntent(normalized)) {
            return "BUILD";
        }
        if (normalized.matches(".*\\b\\d{8,14}\\b.*")) {
            return "SHOPPING";
        }
        for (String kw : SHOPPING_KEYWORDS) {
            if (normalized.contains(kw)) {
                return "SHOPPING";
            }
        }
        // Si hay conversación activa de compras y el usuario hace una pregunta corta de seguimiento ("¿y ese?", "¿cuál mejor?")
        if (history != null && !history.isEmpty() && normalized.length() <= 60
                && (normalized.contains("ese") || normalized.contains("esa") || normalized.contains("cual")
                || normalized.contains("cuanto") || normalized.contains("otro") || normalized.contains("otra")
                || normalized.contains("mas ") || normalized.contains("diferencia") || normalized.contains("conviene"))) {
            return "SHOPPING";
        }
        return "OFF_TOPIC";
    }

    private AiProvider.AiResponse invokeWithFallback(AiProvider.AiRequest request) {
        if ("gemini".equalsIgnoreCase(configuredProvider) && geminiProvider.isConfigured()) {
            try {
                return geminiProvider.chat(request);
            } catch (RuntimeException ex) {
                LOG.warn("Fallo al invocar Gemini ({}); usando respaldo MockAiProvider.", ex.getMessage());
                return mockProvider.chat(request);
            }
        }
        return mockProvider.chat(request);
    }

    @SuppressWarnings("unchecked")
    private List<AiTool.ToolResult> collectEvidence(String query, boolean buildMode, Long budgetCop, String category) {
        List<AiTool.ToolResult> results = new ArrayList<>();
        String lower = normalizeAscii(query);

        if (lower.contains("tienda") || lower.contains("envio")) {
            toolRegistry.find("listar_tiendas").ifPresent(t -> results.add(t.execute(Map.of())));
        }
        if (lower.contains("como funciona") || lower.contains("ayuda") || lower.contains("demo")) {
            toolRegistry.find("consultar_ayuda").ifPresent(t -> results.add(t.execute(Map.of())));
        }

        if (buildMode) {
            long effectiveBudget = (budgetCop != null && budgetCop > 0) ? budgetCop : 3_000_000L;
            var itemsResult = toolRegistry.find("buscar_componentes")
                    .map(t -> t.execute(Map.of("query", extractKeyword(query), "budgetCop", effectiveBudget)))
                    .orElse(null);
            if (itemsResult != null) {
                results.add(itemsResult);
                List<SearchService.ComparedOffer> items = itemsResult.data() instanceof List<?> l
                        ? (List<SearchService.ComparedOffer>) l
                        : List.of();
                toolRegistry.find("calcular_total")
                        .ifPresent(t -> results.add(t.execute(Map.of("items", items))));
                toolRegistry.find("validar_compatibilidad")
                        .ifPresent(t -> results.add(t.execute(Map.of(
                                "category", category == null ? "GENERAL" : category,
                                "budgetCop", effectiveBudget,
                                "items", items))));
            }
            return results;
        }

        String keyword = extractKeyword(query);
        toolRegistry.find("buscar_productos").ifPresent(t -> results.add(t.execute(Map.of("query", keyword))));
        toolRegistry.find("comparar_ofertas").ifPresent(t -> results.add(t.execute(Map.of("query", keyword))));
        if (lower.contains("esperar") || lower.contains("comprar") || lower.contains("historial") || lower.contains("baj")) {
            toolRegistry.find("historial_precios").ifPresent(t -> results.add(t.execute(Map.of("query", keyword))));
            toolRegistry.find("prediccion_compra").ifPresent(t -> results.add(t.execute(Map.of("query", keyword))));
        }
        return results;
    }

    private boolean looksLikeBuildIntent(String normalized) {
        return normalized.contains("armar") || normalized.contains("arma ") || normalized.contains("setup")
                || normalized.contains("kit") || normalized.contains("presupuesto") || normalized.contains("combo");
    }

    private String extractKeyword(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        for (String known : List.of("sony", "wh-1000xm5", "audífonos", "audifonos", "logitech", "g502", "mouse",
                "ninja", "freidora", "hub", "usb", "nutella")) {
            if (lower.contains(known)) {
                return known;
            }
        }
        return message.length() > 40 ? message.substring(0, 40) : message;
    }

    private String normalizeAscii(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
    }
}
