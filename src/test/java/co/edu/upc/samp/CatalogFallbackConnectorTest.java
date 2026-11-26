package co.edu.upc.samp;

import co.edu.upc.samp.entidades.ConnectorKind;
import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.StoreConnectorConfig;
import co.edu.upc.samp.servicios.conectores.CatalogFallbackConnector;
import co.edu.upc.samp.servicios.conectores.SuccessRatePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del conector de respaldo {@code DEMO} y de la política de selección de conectores.
 */
class CatalogFallbackConnectorTest {

    private final CatalogFallbackConnector fallbackConnector = new CatalogFallbackConnector();
    private final SuccessRatePolicy policy = new SuccessRatePolicy();

    @Test
    void shouldFindOffersByTextAndMarkAsDemo() {
        List<RawOffer> results = fallbackConnector.search(SearchQuery.byText("Sony WH-1000XM5"));
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(o -> o.dataKind() == DataKind.DEMO);
        assertThat(results).anyMatch(o -> !o.shippingKnown());
    }

    @Test
    void shouldFindOfferByGtin() {
        assertThat(fallbackConnector.fetchByGtin("3017620422003"))
                .isPresent()
                .get()
                .extracting(RawOffer::title)
                .asString()
                .containsIgnoringCase("Nutella");
    }

    @Test
    void selectionPolicyShouldSkipOpenCircuitsAndOrderByPriority() {
        Store ml = new Store("MERCADOLIBRE_CO", "CO", "Mercado Libre Colombia",
                "https://www.mercadolibre.com.co", "COP", DataKind.DEMO);
        StoreConnectorConfig primary = new StoreConnectorConfig(ml, "MercadoLibreScraperConnector", ConnectorKind.SCRAPER, 1);
        StoreConnectorConfig backup = new StoreConnectorConfig(ml, "CatalogFallbackConnector", ConnectorKind.FALLBACK, 2);

        primary.recordFailure();
        primary.recordFailure();
        primary.recordFailure();

        List<StoreConnectorConfig> ranked = policy.rank(List.of(backup, primary));
        assertThat(ranked).containsExactly(backup);
    }
}
