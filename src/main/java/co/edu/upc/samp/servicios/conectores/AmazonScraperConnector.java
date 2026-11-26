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
 * Conector de scraping respetuoso para Amazon con envío a Colombia ({@code AMAZON_CO}).
 * Si Amazon restringe el acceso o exige captcha, falla de forma controlada para activar respaldo DEMO.
 */
@Component
public class AmazonScraperConnector extends ScrapingStoreConnector {

    public AmazonScraperConnector(
            RobotsPolicy robotsPolicy,
            @Value("${samp.scraper.user-agent:SAMPBot/1.0}") String userAgent
    ) {
        super(robotsPolicy, userAgent);
    }

    @Override
    public String storeCode() {
        return "AMAZON_CO";
    }

    @Override
    protected String buildSearchUrl(SearchQuery query) {
        String term = query.hasGtin() ? query.gtin() : query.text();
        return "https://www.amazon.com/s?k=" + URLEncoder.encode(term, StandardCharsets.UTF_8);
    }

    @Override
    protected List<RawOffer> parseDocument(Document document, SearchQuery query) {
        List<RawOffer> results = new ArrayList<>();
        Elements cards = document.select("div[data-component-type='s-search-result']");
        for (Element card : cards) {
            if (results.size() >= query.limit()) {
                break;
            }
            String asin = card.attr("data-asin");
            Element titleEl = card.selectFirst("h2 a span");
            Element linkEl = card.selectFirst("h2 a");
            Element priceEl = card.selectFirst("span.a-price span.a-offscreen");
            if (titleEl == null || linkEl == null || priceEl == null) {
                continue;
            }
            BigDecimal price = parsePrice(priceEl.text(), "USD");
            if (price == null) {
                continue;
            }
            Element imgEl = card.selectFirst("img.s-image");
            boolean freeEligible = price.compareTo(new BigDecimal("35.00")) >= 0;

            results.add(new RawOffer(
                    storeCode(),
                    asin.isBlank() ? "AMZ-" + results.size() : asin,
                    titleEl.text().trim(),
                    linkEl.absUrl("href"),
                    price,
                    "USD",
                    freeEligible ? BigDecimal.ZERO : null,
                    freeEligible,
                    null,
                    false,
                    "IN_STOCK",
                    null,
                    null,
                    query.hasGtin() ? query.gtin() : null,
                    DataKind.REAL,
                    imgEl != null ? List.of(imgEl.absUrl("src")) : List.of(),
                    Map.of("envio_colombia", freeEligible ? "Elegible gratis >$35 USD" : "Desconocido"),
                    Instant.now()
            ));
        }
        return results;
    }
}
