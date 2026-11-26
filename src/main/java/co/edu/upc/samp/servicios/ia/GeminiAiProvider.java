package co.edu.upc.samp.servicios.ia;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Proveedor de IA conectado a Google Gemini mediante su API REST oficial.
 *
 * <p>Combina conversación natural en lenguaje humano con instrucciones de sistema estrictas
 * para el dominio de SAMP: saluda y se despide con naturalidad, responde consultas sobre
 * productos, precios y listas con presupuesto usando la evidencia del catálogo, y redirige
 * amablemente preguntas fuera de tema con un mensaje claro.
 */
@Component
public class GeminiAiProvider implements AiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiAiProvider.class);

    private static final String SYSTEM_INSTRUCTION = """
            Eres el asistente virtual de compras de SAMP (Sistema Automatizado de Mejores Precios) para Colombia.
            Hablas en español natural, cercano, amable y humano, como un buen asesor de compras.

            Reglas obligatorias de comportamiento:
            1. Saludos, despedidas y cortesía: Si el usuario te saluda ("hola", "buenas", "¿cómo estás?"), te da las gracias o se despide ("chao", "adiós", "hasta luego"), respóndele de forma natural, cordial y breve como una persona normal, recordándole sutilmente que estás aquí cuando necesite ayuda con productos o precios.
            2. Consultas sobre productos, tiendas, compras y armados:
               - Responde con claridad y naturalidad a cualquier pregunta relacionada con productos, características, comparaciones, recomendaciones, presupuestos, tiendas en Colombia (Mercado Libre, Amazon con envío a Colombia, Temu, Falabella y AliExpress) o cómo funciona SAMP.
               - Cuando se te entregue "Evidencia verificada del catálogo de SAMP", úsala como fuente exacta para precios, tiendas, recomendación de 30 días y totales. NUNCA inventes precios ni disponibilidad.
               - Señala de forma transparente si un dato es de catálogo DEMO o si el costo de envío/impuestos es desconocido (nunca lo asumas como gratis o cero).
               - Si el usuario pregunta por un producto o categoría que aún no tiene ofertas en la evidencia actual, puedes orientarlo con consejos generales útiles sobre qué revisar al comprarlo, aclarando honestamente que SAMP no tiene precios cargados de ese producto en este momento.
               - Recuerda que SAMP no vende ni procesa pagos; siempre se compra en la tienda externa.
            3. Preguntas fuera de tema o sin relación con tu función:
               - Si el usuario hace preguntas raras, sin sentido o totalmente ajenas a productos, compras, precios o SAMP (por ejemplo: política, tareas escolares, recetas de cocina, chistes, consejos médicos/legales, adivinanzas, etc.), NO intentes responder ese tema.
               - Responde amablemente usando este mensaje predeterminado:
                 "Soy el asistente de compras de SAMP y estoy aquí esperando tu pregunta relacionada con productos, comparación de precios en tiendas de Colombia o para ayudarte a armar una lista con tu presupuesto. ¿Qué producto te gustaría consultar hoy?"
            4. Formato: Usa texto limpio y fácil de leer, sin encabezados robóticos innecesarios ni listas excesivas.
            """;

    private final String apiKey;
    private final String primaryModel;
    private volatile HttpClient httpClient;

    /**
     * Inicializa el proveedor Gemini con configuración inyectada.
     * El cliente HTTP se crea bajo demanda únicamente cuando existe clave configurada.
     *
     * @param apiKey clave de API de Google AI Studio (opcional)
     * @param model  identificador del modelo Gemini principal
     */
    public GeminiAiProvider(
            @Value("${samp.ai.gemini-api-key:}") String apiKey,
            @Value("${samp.ai.model:gemini-3.1-flash-lite}") String model
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.primaryModel = (model == null || model.isBlank()) ? "gemini-3.1-flash-lite" : model.trim();
    }

    private HttpClient client() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(8))
                            .build();
                }
            }
        }
        return httpClient;
    }

    @Override
    public String id() {
        return "gemini";
    }

    /**
     * Indica si el proveedor cuenta con clave de API configurada.
     *
     * @return {@code true} si la clave está presente
     */
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public AiResponse chat(AiRequest request) {
        if (!isConfigured()) {
            throw new IllegalStateException("La clave GEMINI_API_KEY no está configurada.");
        }

        List<String> candidateModels = buildModelFallbackOrder();
        RuntimeException lastError = null;

        for (String candidateModel : candidateModels) {
            try {
                return callModel(candidateModel, request);
            } catch (RuntimeException ex) {
                lastError = ex;
                LOG.warn("Modelo Gemini '{}' no disponible ({}); probando siguiente alternativa.",
                        candidateModel, ex.getMessage());
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("Ningún modelo Gemini respondió.");
    }

    private List<String> buildModelFallbackOrder() {
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(primaryModel);
        ordered.add("gemini-3.1-flash-lite");
        ordered.add("gemini-3.8-flash");
        return List.copyOf(ordered);
    }

    private AiResponse callModel(String targetModel, AiRequest request) {
        long start = System.currentTimeMillis();
        JsonObject body = buildPayload(request);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + targetModel + ":generateContent?key=" + apiKey;

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " en modelo " + targetModel);
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini devolvió una respuesta sin candidatos.");
            }
            JsonArray parts = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts");
            StringBuilder answer = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();
                if (part.has("text")) {
                    answer.append(part.get("text").getAsString());
                }
            }
            String text = answer.toString().trim();
            if (text.isBlank()) {
                throw new IllegalStateException("Gemini devolvió texto vacío.");
            }

            long elapsed = System.currentTimeMillis() - start;
            return new AiResponse(id(), targetModel, text, Math.max(20, text.length() / 4), elapsed);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Error de red al consultar Gemini (" + targetModel + ").", e);
        }
    }

    private JsonObject buildPayload(AiRequest request) {
        JsonObject body = new JsonObject();

        // Instrucción de sistema oficial de Gemini
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", SYSTEM_INSTRUCTION);
        JsonArray sysParts = new JsonArray();
        sysParts.add(sysPart);
        JsonObject sysInstruction = new JsonObject();
        sysInstruction.add("parts", sysParts);
        body.add("system_instruction", sysInstruction);

        // Historial reciente + turno actual con evidencia verificada si aplica
        JsonArray contents = new JsonArray();
        if (request.history() != null) {
            int from = Math.max(0, request.history().size() - 6);
            for (ConversationTurn turn : request.history().subList(from, request.history().size())) {
                if (turn != null && turn.text() != null && !turn.text().isBlank()) {
                    String role = "model".equalsIgnoreCase(turn.role()) ? "model" : "user";
                    contents.add(buildContentTurn(role, turn.text()));
                }
            }
        }
        contents.add(buildContentTurn("user", buildCurrentTurnText(request)));
        body.add("contents", contents);

        // Configuración conversacional natural
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.65);
        genConfig.addProperty("maxOutputTokens", 650);
        body.add("generationConfig", genConfig);

        return body;
    }

    private JsonObject buildContentTurn(String role, String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject turn = new JsonObject();
        turn.addProperty("role", role);
        turn.add("parts", parts);
        return turn;
    }

    private String buildCurrentTurnText(AiRequest request) {
        if ("GREETING".equals(request.intent())
                || "FAREWELL".equals(request.intent())
                || "THANKS".equals(request.intent())) {
            return request.userMessage();
        }
        if ("OFF_TOPIC".equals(request.intent())) {
            return "Mensaje del usuario (posiblemente fuera del ámbito de compras/productos): "
                    + request.userMessage();
        }

        String evidenceBlock = request.toolEvidence().stream()
                .map(r -> "- " + r.toolName() + ": " + r.summary())
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();
        sb.append("Mensaje del usuario: ").append(request.userMessage()).append("\n");
        sb.append("Contexto -> Modo: ").append(request.mode())
                .append(" | Categoría: ").append(request.category() == null ? "General" : request.category())
                .append(" | Presupuesto COP: ").append(request.budgetCop() == null ? "Sin límite" : request.budgetCop())
                .append("\n");
        if (!evidenceBlock.isBlank()) {
            sb.append("\nEvidencia verificada del catálogo de SAMP:\n").append(evidenceBlock);
        }
        return sb.toString();
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
