package co.edu.upc.samp.datos.archivo;

import co.edu.upc.samp.datos.StoreRepositoryPort;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de tiendas con persistencia en archivo JSON y colección {@link ArrayList}.
 */
public class JsonStoreRepository extends AbstractJsonFileRepository<Store, Long> implements StoreRepositoryPort {

    /**
     * Crea el repositorio de tiendas apuntando al archivo {@code stores.json} del directorio dado.
     *
     * @param dataDirectory carpeta de almacenamiento JSON
     */
    public JsonStoreRepository(Path dataDirectory) {
        super(
                dataDirectory.resolve("stores.json"),
                new TypeToken<ArrayList<Store>>() {}.getType(),
                "tienda"
        );
    }

    @Override
    protected Long extractId(Store entity) {
        return entity.getId();
    }

    @Override
    protected Long assignNextId(Store entity) {
        long next = records.stream()
                .map(Store::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
        entity.setId(next);
        return next;
    }

    @Override
    protected void validateUniqueConstraints(Store entity, Long currentId) {
        boolean duplicateCode = records.stream()
                .anyMatch(s -> s.getCode().equalsIgnoreCase(entity.getCode())
                        && !Objects.equals(s.getId(), currentId));
        if (duplicateCode) {
            throw new DuplicateEntityException("tienda", "código", entity.getCode());
        }
    }

    @Override
    public synchronized Optional<Store> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return records.stream()
                .filter(s -> s.getCode().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<Store> findAllActive() {
        return records.stream().filter(Store::isActive).toList();
    }
}
