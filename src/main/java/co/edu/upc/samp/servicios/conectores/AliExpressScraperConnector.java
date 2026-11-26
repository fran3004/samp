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
 * Conector de scraping respetuoso para AliExpress Choice ({@code ALIEXPRESS_CO}).
 * Ante bloqueos o captcha, delega automáticamente al respaldo DEMO.
 */
@Component
public class AliExpressScraperConnector extends ScrapingStoreConnector {

    public AliExpressScraperConnector(
            RobotsPolicy robotsPolicy,
            @Value("${samp.scraper.user-agent:SAMPBot/1.0}") String userAgent
    ) {
        super(robotsPolicy, userAgent);
    }

    @Override
    public String storeCode() {
        return "ALIEXPRESS_CO";
    }

    @Override
    protected String buildSearchUrl(SearchQuery query) {
        String term = query.hasGtin() ? query.gtin() : query.text();
        String slug = URLEncoder.encode(term, StandardCharsets.UTF_8).replace("+", "-");
        return "https://es.aliexpress.com/w/wholesale-" + slug + ".html";
    }

    @Override
    protected List<RawOffer> parseDocument(Document document, SearchQuery query) {
        List<RawOffer> results = new ArrayList<>();
        Elements cards = document.select("div.search-item-card-wrapper-gallery, a.search-card-item");
        for (Element card : cards) {
            if (results.size() >= query.limit()) {
                break;
            }
            Element titleEl = card.selectFirst("h3, h1");
            Element priceEl = card.selectFirst("div[class*='price-sale'], span[class*='price']");
            if (titleEl == null || priceEl == null) {
                continue;
            }
            BigDecimal price = parsePrice(priceEl.text(), "USD");
            if (price == null) {
                continue;
            }
            results.add(new RawOffer(
                    storeCode(),
                    "ALI-" + Math.abs(titleEl.text().hashCode()),
                    titleEl.text().trim(),
                    card.absUrl("href"),
                    price,
                    "USD",
                    null,
                    false,
                    null,
                    false,
                    "IN_STOCK",
                    null,
                    null,
                    query.hasGtin() ? query.gtin() : null,
                    DataKind.REAL,
                    List.of(),
                    Map.of("modalidad", "Choice 10-15 días"),
                    Instant.now()
            ));
        }
        return results;
    }
}
