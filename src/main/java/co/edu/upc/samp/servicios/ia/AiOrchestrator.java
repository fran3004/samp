package co.edu.upc.samp.servicios.ia;

import co.edu.upc.samp.servicios.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Orquestador central del asistente de IA general de SAMP (chat de consultas + modo armar).
 *
 * <p>Ejecuta en orden: {@link AiGuardrails} -> herramientas de solo lectura ({@link ToolRegistry})
 * -> {@link AiProvider} configurado (Gemini con respaldo automático a {@link MockAiProvider})
 * -> {@link OutputValidator}.
 */
@Service
public class AiOrchestrator {

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
     * Atiende una consulta libre sobre productos o una solicitud para armar cualquier configuración.
     *
     * @param rawMessage pregunta o necesidad del usuario
     * @param mode       {@code CHAT} o {@code BUILD}
     * @param budgetCop  presupuesto máximo opcional en COP
     * @param category   categoría libre opcional
     * @return respuesta trazable con herramientas invocadas y validación
     */
    public ChatReply ask(String rawMessage, String mode, Long budgetCop, String category) {
        AiGuardrails.GuardResult guard = guardrails.screen(rawMessage);
        if (!guard.allowed()) {
            return new ChatReply(guard.reason(), "guardrails", "policy", List.of(), false, 1L);
        }

        String sanitized = guard.sanitized();
        boolean buildMode = "BUILD".equalsIgnoreCase(mode) || looksLikeBuildIntent(sanitized);
        List<AiTool.ToolResult> evidence = collectEvidence(sanitized, buildMode, budgetCop, category);
        List<String> toolsInvoked = evidence.stream().map(AiTool.ToolResult::toolName).toList();

        AiProvider.AiRequest request = new AiProvider.AiRequest(
                sanitized,
                buildMode ? "BUILD" : "CHAT",
                budgetCop,
                category,
                evidence
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

    private AiProvider.AiResponse invokeWithFallback(AiProvider.AiRequest request) {
        if ("gemini".equalsIgnoreCase(configuredProvider) && geminiProvider.isConfigured()) {
            try {
                return geminiProvider.chat(request);
            } catch (RuntimeException ignored) {
                return mockProvider.chat(request);
            }
        }
        return mockProvider.chat(request);
    }

    @SuppressWarnings("unchecked")
    private List<AiTool.ToolResult> collectEvidence(String query, boolean buildMode, Long budgetCop, String category) {
        List<AiTool.ToolResult> results = new ArrayList<>();
        String lower = query.toLowerCase(Locale.ROOT);

        if (lower.contains("tienda") || lower.contains("envío") || lower.contains("envio")) {
            toolRegistry.find("listar_tiendas").ifPresent(t -> results.add(t.execute(Map.of())));
        }
        if (lower.contains("cómo funciona") || lower.contains("como funciona") || lower.contains("ayuda") || lower.contains("demo")) {
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

    private boolean looksLikeBuildIntent(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("armar") || lower.contains("arma ") || lower.contains("setup")
                || lower.contains("kit") || lower.contains("presupuesto") || lower.contains("combo");
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
}
