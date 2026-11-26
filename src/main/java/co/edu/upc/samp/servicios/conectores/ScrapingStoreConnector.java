package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.ConnectorKind;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.entidades.excepciones.ConnectorException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Clase abstracta para conectores basados en scraping HTML con Jsoup.
 *
 * <p>Verifica {@code robots.txt}, envía un {@code User-Agent} identificable, detecta
 * respuestas de bloqueo ({@code 403}, {@code 429} o captcha) sin intentar evadirlas
 * y delega el parseo del documento HTML a cada tienda concreta.
 */
public abstract class ScrapingStoreConnector extends AbstractStoreConnector {

    private static final int TIMEOUT_MS = 8000;
    private final RobotsPolicy robotsPolicy;
    private final String userAgent;

    /**
     * Inicializa el conector de scraping con su política de {@code robots.txt}.
     *
     * @param robotsPolicy validador de reglas {@code robots.txt}
     * @param userAgent    identificador HTTP de SAMP
     */
    protected ScrapingStoreConnector(RobotsPolicy robotsPolicy, String userAgent) {
        this.robotsPolicy = robotsPolicy;
        this.userAgent = userAgent;
    }

    @Override
    public ConnectorKind kind() {
        return ConnectorKind.SCRAPER;
    }

    /**
     * Construye la URL de búsqueda de la tienda para la consulta dada.
     *
     * @param query consulta de búsqueda
     * @return URL completa de resultados
     */
    protected abstract String buildSearchUrl(SearchQuery query);

    /**
     * Extrae ofertas crudas del documento HTML descargado.
     *
     * @param document documento HTML parseado por Jsoup
     * @param query    consulta original
     * @return lista de ofertas encontradas
     */
    protected abstract List<RawOffer> parseDocument(Document document, SearchQuery query);

    @Override
    protected List<RawOffer> executeSearch(SearchQuery query) {
        String searchUrl = buildSearchUrl(query);
        if (!robotsPolicy.isAllowed(searchUrl, userAgent)) {
            throw new ConnectorException(storeCode(), "Ruta restringida por robots.txt: " + searchUrl, null);
        }
        try {
            Connection.Response response = Jsoup.connect(searchUrl)
                    .userAgent(userAgent)
                    .header("Accept-Language", "es-CO,es;q=0.9")
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();
            if (status == 403 || status == 429 || status == 503) {
                throw new ConnectorException(storeCode(), "Acceso restringido por la tienda (HTTP " + status + ").", null);
            }
            if (status >= 400) {
                throw new ConnectorException(storeCode(), "Respuesta HTTP no exitosa: " + status, null);
            }

            Document document = response.parse();
            String lowerHtml = document.text().toLowerCase();
            if (lowerHtml.contains("captcha") || lowerHtml.contains("robot check") || lowerHtml.contains("verify you are human")) {
                throw new ConnectorException(storeCode(), "La tienda solicitó verificación anti-bot; no se evade.", null);
            }
            return parseDocument(document, query);
        } catch (IOException e) {
            throw new ConnectorException(storeCode(), "Error de red al consultar la tienda.", e);
        }
    }

    /**
     * Limpia cadenas de precio en formato colombiano o internacional (por ejemplo, {@code $ 1.299.900} o {@code 49.99}).
     *
     * @param rawPrice texto crudo de precio
     * @param currency moneda esperada
     * @return valor numérico parseado o {@code null} si no es válido
     */
    protected BigDecimal parsePrice(String rawPrice, String currency) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return null;
        }
        String cleaned = rawPrice.replaceAll("[^0-9.,]", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            if ("COP".equalsIgnoreCase(currency)) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
