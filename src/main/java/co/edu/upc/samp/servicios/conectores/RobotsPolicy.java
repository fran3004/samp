package co.edu.upc.samp.servicios.conectores;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Política de verificación de {@code robots.txt} para scraping ético y transparente.
 *
 * <p>Respeta estrictamente las directivas {@code Disallow} para {@code User-agent: *}.
 * Nunca evade bloqueos ni captchas; ante cualquier restricción, el conector falla de
 * forma controlada para activar el respaldo {@code DEMO}.
 */
@Component
public class RobotsPolicy {

    private static final int TIMEOUT_MS = 5000;
    private final Map<String, List<String>> disallowedCache = new ConcurrentHashMap<>();

    /**
     * Comprueba si una ruta de consulta está permitida por el {@code robots.txt} del dominio.
     *
     * @param targetUrl URL objetivo a consultar
     * @param userAgent identificador del agente HTTP de SAMP
     * @return {@code true} si no coincide con ninguna regla {@code Disallow}
     */
    public boolean isAllowed(String targetUrl, String userAgent) {
        try {
            URI uri = URI.create(targetUrl);
            String origin = uri.getScheme() + "://" + uri.getHost();
            List<String> disallowedPaths = disallowedCache.computeIfAbsent(origin, o -> fetchDisallowedPaths(o, userAgent));
            String path = uri.getRawPath() == null ? "/" : uri.getRawPath();
            for (String rule : disallowedPaths) {
                if ("/".equals(rule) || (!rule.isBlank() && path.startsWith(rule))) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private List<String> fetchDisallowedPaths(String origin, String userAgent) {
        List<String> rules = new ArrayList<>();
        try {
            Connection.Response response = Jsoup.connect(origin + "/robots.txt")
                    .userAgent(userAgent)
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .execute();
            if (response.statusCode() >= 400) {
                // Si el sitio bloquea incluso robots.txt, se asume restringido para no insistir.
                rules.add("/");
                return rules;
            }
            boolean inWildcardBlock = false;
            for (String rawLine : response.body().split("\\R")) {
                String line = rawLine.split("#", 2)[0].trim();
                if (line.isEmpty()) {
                    continue;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith("user-agent:")) {
                    String agent = line.substring("user-agent:".length()).trim();
                    inWildcardBlock = "*".equals(agent);
                } else if (inWildcardBlock && lower.startsWith("disallow:")) {
                    String path = line.substring("disallow:".length()).trim();
                    if (!path.isEmpty()) {
                        rules.add(path);
                    }
                }
            }
        } catch (IOException e) {
            // Sin acceso a robots.txt: se restringe por precaución y actúa el respaldo DEMO.
            rules.add("/");
        }
        return rules;
    }
}
