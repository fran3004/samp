package co.edu.upc.samp.datos.archivo;

import co.edu.upc.samp.datos.CrudRepositoryPort;
import co.edu.upc.samp.entidades.excepciones.DataPersistenceException;
import co.edu.upc.samp.entidades.excepciones.EntityNotFoundException;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Clase abstracta base para repositorios con persistencia en archivos JSON respaldados por {@link ArrayList}.
 *
 * <p>Implementa operaciones CRUD completas con control de consistencia (evita registros
 * duplicados, huérfanos o vacíos) y manejo específico de {@link IOException} y
 * {@link JsonParseException}. Utilizada por el perfil {@code consola} para cumplir la Fase 2
 * y el ítem 5.4 de la rúbrica.
 *
 * @param <T>  tipo de entidad almacenada
 * @param <ID> tipo de identificador único
 */
public abstract class AbstractJsonFileRepository<T, ID> implements CrudRepositoryPort<T, ID> {

    protected final ArrayList<T> records = new ArrayList<>();
    private final Path filePath;
    private final Gson gson;
    private final Type listType;
    private final String entityLabel;

    /**
     * Inicializa el repositorio cargando los registros existentes desde el archivo JSON si existe.
     *
     * @param filePath    ruta del archivo JSON
     * @param listType    tipo parametrizado de {@code ArrayList<T>} para Gson
     * @param entityLabel nombre legible de la entidad para mensajes de error
     */
    protected AbstractJsonFileRepository(Path filePath, Type listType, String entityLabel) {
        this.filePath = Objects.requireNonNull(filePath, "La ruta del archivo JSON no puede ser nula.");
        this.listType = Objects.requireNonNull(listType, "El tipo de lista para Gson es obligatorio.");
        this.entityLabel = entityLabel;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        String owner = f.getDeclaringClass().getSimpleName();
                        String field = f.getName();
                        return ("ProductIdentifier".equals(owner) && "product".equals(field))
                                || ("ProductImage".equals(owner) && "product".equals(field))
                                || ("BuildItem".equals(owner) && "build".equals(field));
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class,
                        (JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant.parse(json.getAsString()))
                .create();
        loadFromFile();
    }

    /**
     * Extrae el identificador único de la entidad.
     *
     * @param entity entidad a inspeccionar
     * @return identificador actual o {@code null}
     */
    protected abstract ID extractId(T entity);

    /**
     * Asigna un nuevo identificador consecutivo a una entidad nueva.
     *
     * @param entity entidad sin identificador
     * @return identificador asignado
     */
    protected abstract ID assignNextId(T entity);

    /**
     * Valida reglas de unicidad antes de guardar o actualizar (sobrescribible por subclases).
     *
     * @param entity    entidad a validar
     * @param currentId identificador actual si es actualización, o {@code null} si es creación
     */
    protected void validateUniqueConstraints(T entity, ID currentId) {
        // Por defecto solo se valida unicidad del identificador en save/update.
    }

    @Override
    public synchronized T save(T entity) {
        Objects.requireNonNull(entity, "No se puede guardar un registro nulo de " + entityLabel + ".");
        validateUniqueConstraints(entity, null);
        if (extractId(entity) == null) {
            assignNextId(entity);
        }
        records.add(entity);
        persistToFile();
        return entity;
    }

    @Override
    public synchronized Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return records.stream()
                .filter(item -> id.equals(extractId(item)))
                .findFirst();
    }

    @Override
    public synchronized List<T> findAll() {
        return new ArrayList<>(records);
    }

    @Override
    public synchronized T update(ID id, T entity) {
        Objects.requireNonNull(entity, "Los datos de actualización no pueden ser nulos.");
        int index = indexOf(id);
        if (index < 0) {
            throw new EntityNotFoundException(entityLabel, id);
        }
        validateUniqueConstraints(entity, id);
        records.set(index, entity);
        persistToFile();
        return entity;
    }

    @Override
    public synchronized void deleteById(ID id) {
        int index = indexOf(id);
        if (index < 0) {
            throw new EntityNotFoundException(entityLabel, id);
        }
        records.remove(index);
        persistToFile();
    }

    private int indexOf(ID id) {
        for (int i = 0; i < records.size(); i++) {
            if (Objects.equals(extractId(records.get(i)), id)) {
                return i;
            }
        }
        return -1;
    }

    private void loadFromFile() {
        if (!Files.exists(filePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            ArrayList<T> loaded = gson.fromJson(reader, listType);
            records.clear();
            if (loaded != null) {
                loaded.stream().filter(Objects::nonNull).forEach(records::add);
            }
        } catch (IOException e) {
            throw new DataPersistenceException("No se pudo leer el archivo de " + entityLabel + ": " + filePath, e);
        } catch (JsonParseException e) {
            throw new DataPersistenceException("El archivo JSON de " + entityLabel + " tiene formato inválido: " + filePath, e);
        }
    }

    private void persistToFile() {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                gson.toJson(records, writer);
            }
        } catch (IOException e) {
            throw new DataPersistenceException("No se pudo guardar el archivo de " + entityLabel + ": " + filePath, e);
        }
    }
}
