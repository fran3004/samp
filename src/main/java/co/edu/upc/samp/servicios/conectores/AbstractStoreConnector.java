package co.edu.upc.samp.servicios.conectores;

import co.edu.upc.samp.entidades.RawOffer;
import co.edu.upc.samp.entidades.SearchQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Clase abstracta base para todos los conectores de tienda de SAMP.
 *
 * <p>Aplica el patrón Template Method: controla el flujo común de búsqueda,
 * filtra ofertas inválidas (precio negativo o título vacío) y delega la
 * obtención concreta en {@link #executeSearch(SearchQuery)}.
 */
public abstract class AbstractStoreConnector implements StoreConnector {

    @Override
    public final List<RawOffer> search(SearchQuery query) {
        if (query == null) {
            return List.of();
        }
        return executeSearch(query).stream()
                .filter(this::isValidOffer)
                .limit(query.limit())
                .toList();
    }

    @Override
    public Optional<RawOffer> fetchByGtin(String gtin) {
        if (gtin == null || gtin.isBlank()) {
            return Optional.empty();
        }
        return search(SearchQuery.byGtin(gtin)).stream()
                .filter(o -> gtin.trim().equalsIgnoreCase(o.gtin()))
                .findFirst();
    }

    /**
     * Ejecuta la búsqueda específica de la subclase (scraping, API o catálogo demo).
     *
     * @param query consulta normalizada
     * @return ofertas crudas obtenidas
     */
    protected abstract List<RawOffer> executeSearch(SearchQuery query);

    /**
     * Verifica calidad mínima de una oferta antes de pasarla a normalización.
     *
     * @param offer oferta cruda a validar
     * @return {@code true} si cumple los campos mínimos obligatorios
     */
    protected boolean isValidOffer(RawOffer offer) {
        return offer != null
                && offer.title() != null && !offer.title().isBlank()
                && offer.price() != null && offer.price().compareTo(BigDecimal.ZERO) > 0
                && offer.currency() != null && offer.currency().length() == 3
                && offer.productUrl() != null && !offer.productUrl().isBlank();
    }
}
