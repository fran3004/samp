package co.edu.upc.samp;

import co.edu.upc.samp.datos.archivo.JsonStoreRepository;
import co.edu.upc.samp.datos.archivo.JsonUserRepository;
import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.Role;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import co.edu.upc.samp.entidades.excepciones.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de persistencia CRUD completa y consistencia en archivos JSON con {@code ArrayList}.
 */
class JsonRepositoryCrudTest {

    @TempDir
    Path tempDir;

    @Test
    void storeJsonRepositoryShouldPersistCrudAndPreventDuplicates() {
        JsonStoreRepository repo = new JsonStoreRepository(tempDir);
        Store created = repo.save(new Store(
                "MERCADOLIBRE_CO", "CO", "Mercado Libre Colombia",
                "https://www.mercadolibre.com.co", "COP", DataKind.DEMO));
        assertThat(created.getId()).isEqualTo(1L);

        assertThatThrownBy(() -> repo.save(new Store(
                "MERCADOLIBRE_CO", "CO", "Duplicada",
                "https://www.mercadolibre.com.co", "COP", DataKind.DEMO)))
                .isInstanceOf(DuplicateEntityException.class);

        created.setDataKind(DataKind.REAL);
        repo.update(created.getId(), created);

        // Recarga desde disco para comprobar persistencia real
        JsonStoreRepository reloaded = new JsonStoreRepository(tempDir);
        assertThat(reloaded.findByCode("MERCADOLIBRE_CO"))
                .isPresent()
                .get()
                .extracting(Store::getDataKind)
                .isEqualTo(DataKind.REAL);

        reloaded.deleteById(1L);
        assertThat(reloaded.findAll()).isEmpty();
        assertThatThrownBy(() -> reloaded.deleteById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void userJsonRepositoryShouldPersistRolesAndPreventDuplicateEmails() {
        JsonUserRepository repo = new JsonUserRepository(tempDir);
        User admin = new User("admin@samp.co", "{noop}secreto", "Administrador SAMP");
        admin.addRole(new Role("ADMIN"));
        repo.save(admin);

        JsonUserRepository reloaded = new JsonUserRepository(tempDir);
        assertThat(reloaded.findByEmail("admin@samp.co"))
                .isPresent()
                .get()
                .matches(u -> u.hasRole("ADMIN"));

        assertThatThrownBy(() -> reloaded.save(new User("ADMIN@SAMP.CO", "{noop}otro", "Otro")))
                .isInstanceOf(DuplicateEntityException.class);
    }
}
