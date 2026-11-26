package co.edu.upc.samp.servicios.ia;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Proveedor de IA conectado a Google Gemini mediante su API REST oficial.
 * Si no existe {@code GEMINI_API_KEY} configurada o ocurre un error de red,
 * lanza una excepción controlada para que {@link AiOrchestrator} active {@link MockAiProvider}.
 */
@Component
public class GeminiAiProvider implements AiProvider {

    private final String apiKey;
    private final String model;
    private volatile HttpClient httpClient;

    /**
     * Inicializa el proveedor Gemini con configuración inyectada.
     * El cliente HTTP se crea bajo demanda únicamente cuando existe clave configurada.
     *
     * @param apiKey clave de API de Google AI Studio (opcional)
     * @param model  identificador del modelo Gemini
     */
    public GeminiAiProvider(
            @Value("${samp.ai.gemini-api-key:}") String apiKey,
            @Value("${samp.ai.model:gemini-2.5-flash}") String model
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
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
        long start = System.currentTimeMillis();
        String evidenceBlock = request.toolEvidence().stream()
                .map(r -> r.toolName() + ": " + r.summary())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Eres el asistente de compras de SAMP (Sistema Automatizado de Mejores Precios) para Colombia.
                Responde en español claro y conciso. Usa SOLO los datos de evidencia abajo; nunca inventes precios ni omitas cuando un dato es DEMO o un envío es desconocido.
                Modo: %s | Categoría: %s | Presupuesto COP: %s
                Consulta del usuario: %s

                Evidencia verificada del catálogo:
                %s
                """.formatted(
                request.mode(),
                request.category() == null ? "General" : request.category(),
                request.budgetCop() == null ? "Sin límite" : request.budgetCop(),
                request.userMessage(),
                evidenceBlock
        );

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(textPart);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject body = new JsonObject();
        body.add("contents", contents);

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Gemini respondió con HTTP " + response.statusCode());
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String answer = json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            long elapsed = System.currentTimeMillis() - start;
            return new AiResponse(id(), model, answer, Math.max(40, answer.length() / 4), elapsed);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("No fue posible comunicarse con Gemini.", e);
        }
    }

    @Override
    public boolean supportsVision() {
        return true;
    }
}
