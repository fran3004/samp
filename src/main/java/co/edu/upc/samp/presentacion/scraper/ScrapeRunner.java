package co.edu.upc.samp.presentacion.scraper;

import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.servicios.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ejecutor de ingesta programada para el perfil {@code scraper} ({@code --spring.profiles.active=scraper}).
 *
 * <p>Invocado desde GitHub Actions cada 6 horas sin levantar servidor web. Consulta las 5 tiendas
 * de Colombia respetando {@code robots.txt} y registra el estado de cada fuente.
 */
@Component
@Profile("scraper")
public class ScrapeRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ScrapeRunner.class);
    private static final List<String> TRACKED_QUERIES = List.of(
            "Sony WH-1000XM5", "Logitech G502", "Freidora Ninja", "Hub USB-C"
    );

    private final SearchService searchService;

    /**
     * Inicializa el runner programado con el servicio de búsqueda multi-tienda.
     *
     * @param searchService servicio de búsqueda y respaldo DEMO
     */
    public ScrapeRunner(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void run(String... args) {
        LOG.info("Iniciando ejecución programada de scraping SAMP para 5 tiendas de Colombia...");
        for (String term : TRACKED_QUERIES) {
            var response = searchService.search(SearchQuery.byText(term), true);
            LOG.info("Consulta '{}' -> {} oferta(s) en {} grupo(s)",
                    term, response.totalOffers(), response.groups().size());
            for (var status : response.storeStatuses()) {
                LOG.info("  [{}] {} | {} | {} ofertas | {}",
                        status.storeCode(), status.storeName(), status.dataKind(), status.itemCount(), status.note());
            }
        }
        LOG.info("Ejecución programada finalizada correctamente.");
    }
}
