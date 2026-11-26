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
 * Conector de scraping respetuoso para Falabella Colombia ({@code FALABELLA_CO}).
 */
@Component
public class FalabellaScraperConnector extends ScrapingStoreConnector {

    public FalabellaScraperConnector(
            RobotsPolicy robotsPolicy,
            @Value("${samp.scraper.user-agent:SAMPBot/1.0}") String userAgent
    ) {
        super(robotsPolicy, userAgent);
    }

    @Override
    public String storeCode() {
        return "FALABELLA_CO";
    }

    @Override
    protected String buildSearchUrl(SearchQuery query) {
        String term = query.hasGtin() ? query.gtin() : query.text();
        return "https://www.falabella.com.co/falabella-co/search?Ntt=" + URLEncoder.encode(term, StandardCharsets.UTF_8);
    }

    @Override
    protected List<RawOffer> parseDocument(Document document, SearchQuery query) {
        List<RawOffer> results = new ArrayList<>();
        Elements cards = document.select("div.search-results-4-grid a.pod-link, div[data-pod]");
        for (Element card : cards) {
            if (results.size() >= query.limit()) {
                break;
            }
            Element titleEl = card.selectFirst("b.pod-subTitle, span.pod-subTitle");
            Element priceEl = card.selectFirst("li[data-internet-price], span.copy10");
            if (titleEl == null || priceEl == null) {
                continue;
            }
            BigDecimal price = parsePrice(priceEl.text(), "COP");
            if (price == null) {
                continue;
            }
            String url = card.absUrl("href");
            if (url.isBlank()) {
                url = "https://www.falabella.com.co/falabella-co";
            }
            results.add(new RawOffer(
                    storeCode(),
                    "FAL-" + Math.abs(url.hashCode()),
                    titleEl.text().trim(),
                    url,
                    price,
                    "COP",
                    null,
                    false,
                    BigDecimal.ZERO,
                    true,
                    "IN_STOCK",
                    null,
                    null,
                    query.hasGtin() ? query.gtin() : null,
                    DataKind.REAL,
                    List.of(),
                    Map.of("retiro_tienda", "Gratis en sucursal"),
                    Instant.now()
            ));
        }
        return results;
    }
}
