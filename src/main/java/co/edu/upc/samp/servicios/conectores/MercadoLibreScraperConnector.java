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
 * Conector de scraping respetuoso para Mercado Libre Colombia ({@code MERCADOLIBRE_CO}).
 */
@Component
public class MercadoLibreScraperConnector extends ScrapingStoreConnector {

    public MercadoLibreScraperConnector(
            RobotsPolicy robotsPolicy,
            @Value("${samp.scraper.user-agent:SAMPBot/1.0}") String userAgent
    ) {
        super(robotsPolicy, userAgent);
    }

    @Override
    public String storeCode() {
        return "MERCADOLIBRE_CO";
    }

    @Override
    protected String buildSearchUrl(SearchQuery query) {
        String term = query.hasGtin() ? query.gtin() : query.text();
        String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8).replace("+", "-");
        return "https://listado.mercadolibre.com.co/" + encoded;
    }

    @Override
    protected List<RawOffer> parseDocument(Document document, SearchQuery query) {
        List<RawOffer> results = new ArrayList<>();
        Elements cards = document.select("li.ui-search-layout__item, div.poly-card");
        int index = 0;
        for (Element card : cards) {
            if (index >= query.limit()) {
                break;
            }
            Element titleLink = card.selectFirst("a.poly-component__title, a.ui-search-link__title-card, h2 a");
            Element priceEl = card.selectFirst("span.andes-money-amount__fraction");
            if (titleLink == null || priceEl == null) {
                continue;
            }
            BigDecimal price = parsePrice(priceEl.text(), "COP");
            if (price == null) {
                continue;
            }
            String url = titleLink.absUrl("href");
            Element imgEl = card.selectFirst("img");
            String imgUrl = imgEl != null ? imgEl.absUrl("src") : "";
            boolean freeShipping = card.text().toLowerCase().contains("envío gratis");

            results.add(new RawOffer(
                    storeCode(),
                    "ML-" + Math.abs(url.hashCode()),
                    titleLink.text().trim(),
                    url,
                    price,
                    "COP",
                    freeShipping ? BigDecimal.ZERO : null,
                    freeShipping,
                    BigDecimal.ZERO,
                    true,
                    "IN_STOCK",
                    null,
                    null,
                    query.hasGtin() ? query.gtin() : null,
                    DataKind.REAL,
                    imgUrl.isBlank() ? List.of() : List.of(imgUrl),
                    Map.of("envio", freeShipping ? "Gratis" : "Desconocido"),
                    Instant.now()
            ));
            index++;
        }
        return results;
    }
}
