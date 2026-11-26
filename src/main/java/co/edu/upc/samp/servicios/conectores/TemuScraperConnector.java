package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Conector de scraping respetuoso para Temu Colombia ({@code TEMU_CO}).
 * Ante restricciones de robots.txt o verificación anti-bot, delega al respaldo DEMO.
 */
@Component
public class TemuScraperConnector extends ScrapingStoreConnector {

    public TemuScraperConnector(
            RobotsPolicy robotsPolicy,
            @Value("${samp.scraper.user-agent:SAMPBot/1.0}") String userAgent
    ) {
        super(robotsPolicy, userAgent);
    }

    @Override
    public String storeCode() {
        return "TEMU_CO";
    }

    @Override
    protected String buildSearchUrl(SearchQuery query) {
        String term = query.hasGtin() ? query.gtin() : query.text();
        return "https://www.temu.com/co/search_result.html?search_key=" + URLEncoder.encode(term, StandardCharsets.UTF_8);
    }

    @Override
    protected List<RawOffer> parseDocument(Document document, SearchQuery query) {
        List<RawOffer> results = new ArrayList<>();
        Elements cards = document.select("div[data-tooltip-title], a[href*='goods.html']");
        for (Element card : cards) {
            if (results.size() >= query.limit()) {
                break;
            }
            String title = card.text().trim();
            Element priceEl = card.selectFirst("span[data-type='price']");
            if (title.isBlank() || priceEl == null) {
                continue;
            }
            BigDecimal price = parsePrice(priceEl.text(), "COP");
            if (price == null) {
                continue;
            }
            results.add(new RawOffer(
                    storeCode(),
                    "TEMU-" + Math.abs(title.hashCode()),
                    title,
                    card.absUrl("href"),
                    price,
                    "COP",
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.ZERO,
                    true,
                    "IN_STOCK",
                    null,
                    null,
                    query.hasGtin() ? query.gtin() : null,
                    DataKind.REAL,
                    List.of(),
                    Map.of("envio", "Estándar gratis 1-2 semanas"),
                    Instant.now()
            ));
        }
        return results;
    }
}
