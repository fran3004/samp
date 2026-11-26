package co.edu.upc.samp.datos.archivo;

import co.edu.upc.samp.datos.ProductRepositoryPort;
import co.edu.upc.samp.entidades.Product;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de productos canónicos con persistencia en archivo JSON ({@code products.json})
 * respaldado por {@link ArrayList}. Garantiza unicidad por nombre/modelo y por GTIN.
 */
public class JsonProductRepository extends AbstractJsonFileRepository<Product, Long> implements ProductRepositoryPort {

    /**
     * Crea el repositorio de productos apuntando al archivo {@code products.json}.
     *
     * @param dataDirectory carpeta de almacenamiento JSON
     */
    public JsonProductRepository(Path dataDirectory) {
        super(
                dataDirectory.resolve("products.json"),
                new TypeToken<ArrayList<Product>>() {}.getType(),
                "producto"
        );
    }

    @Override
    protected Long extractId(Product entity) {
        return entity.getId();
    }

    @Override
    protected Long assignNextId(Product entity) {
        long next = records.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
        entity.setId(next);
        return next;
    }

    @Override
    protected void validateUniqueConstraints(Product entity, Long currentId) {
        boolean duplicateName = records.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(entity.getName())
                        && !Objects.equals(p.getId(), currentId));
        if (duplicateName) {
            throw new DuplicateEntityException("producto", "nombre", entity.getName());
        }
    }

    @Override
    public synchronized List<Product> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String lower = query.trim().toLowerCase(Locale.ROOT);
        return records.stream()
                .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(lower)
                        || (p.getModel() != null && p.getModel().toLowerCase(Locale.ROOT).contains(lower)))
                .toList();
    }

    @Override
    public synchronized Optional<Product> findByGtin(String gtin) {
        if (gtin == null || gtin.isBlank()) {
            return Optional.empty();
        }
        String normalized = gtin.trim();
        return records.stream()
                .filter(p -> p.getIdentifiers().stream()
                        .anyMatch(i -> i.getValue().equalsIgnoreCase(normalized)))
                .findFirst();
    }
}
