package co.edu.upc.samp.datos.archivo;

import co.edu.upc.samp.datos.UserRepositoryPort;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de usuarios con persistencia en archivo JSON y colección {@link ArrayList}.
 */
public class JsonUserRepository extends AbstractJsonFileRepository<User, Long> implements UserRepositoryPort {

    /**
     * Crea el repositorio de usuarios apuntando al archivo {@code users.json} del directorio dado.
     *
     * @param dataDirectory carpeta de almacenamiento JSON
     */
    public JsonUserRepository(Path dataDirectory) {
        super(
                dataDirectory.resolve("users.json"),
                new TypeToken<ArrayList<User>>() {}.getType(),
                "usuario"
        );
    }

    @Override
    protected Long extractId(User entity) {
        return entity.getId();
    }

    @Override
    protected Long assignNextId(User entity) {
        long next = records.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
        entity.setId(next);
        return next;
    }

    @Override
    protected void validateUniqueConstraints(User entity, Long currentId) {
        boolean duplicateEmail = records.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(entity.getEmail())
                        && !Objects.equals(u.getId(), currentId));
        if (duplicateEmail) {
            throw new DuplicateEntityException("usuario", "correo electrónico", entity.getEmail());
        }
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return records.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }
}
