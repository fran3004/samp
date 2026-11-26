package co.edu.upc.samp;

import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.Product;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import co.edu.upc.samp.entidades.excepciones.EntityNotFoundException;
import co.edu.upc.samp.entidades.excepciones.ValidationException;
import co.edu.upc.samp.servicios.ConsoleCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas integrales del modo consola ({@link ConsoleCatalogService}):
 * verifica persistencia en archivos JSON ({@code stores.json}, {@code products.json}, {@code users.json}),
 * CRUD completo sin duplicados ni huérfanos, validaciones y recarga desde disco.
 */
class ConsoleModeIntegrationTest {

    @Test
    void seedsInitialJsonFilesAndAuthenticatesRoles(@TempDir Path tempDir) {
        ConsoleCatalogService service = new ConsoleCatalogService(tempDir.toString(), new BCryptPasswordEncoder());

        assertTrue(Files.exists(tempDir.resolve("stores.json")));
        assertTrue(Files.exists(tempDir.resolve("products.json")));
        assertTrue(Files.exists(tempDir.resolve("users.json")));

        assertEquals(5, service.listStores().size());
        assertEquals(5, service.listProducts().size());
        assertEquals(2, service.listUsers().size());

        User admin = service.authenticate("admin@samp.co", "AdminSamp2026!").orElseThrow();
        assertTrue(admin.hasRole("ADMIN"));

        User buyer = service.authenticate("comprador@samp.co", "Comprador2026!").orElseThrow();
        assertFalse(buyer.hasRole("ADMIN"));
        assertTrue(buyer.hasRole("REGISTERED"));
        assertTrue(service.authenticate("admin@samp.co", "ClaveIncorrecta").isEmpty());
    }

    @Test
    void fullCrudOnStoresAndProductsPersistsAcrossReload(@TempDir Path tempDir) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        ConsoleCatalogService service = new ConsoleCatalogService(tempDir.toString(), encoder);

        // Crear tienda y validar duplicado
        Store createdStore = service.createStore("EXITO_CO", "Almacenes Éxito", "https://www.exito.com", "COP", DataKind.DEMO);
        assertNotNull(createdStore.getId());
        assertThrows(DuplicateEntityException.class,
                () -> service.createStore("EXITO_CO", "Duplicada", "https://www.exito.com", "COP", DataKind.DEMO));
        assertThrows(ValidationException.class,
                () -> service.createStore("BAD", "Sin URL", "ftp://invalida", "COP", DataKind.DEMO));

        // Actualizar origen de tienda
        Store updatedStore = service.updateStoreDataKind(createdStore.getId(), DataKind.REAL);
        assertEquals(DataKind.REAL, updatedStore.getDataKind());

        // Crear, actualizar y verificar producto con GTIN (sin ciclos Gson)
        Product createdProduct = service.createProduct("Teclado Mecánico Keychron K2", "Keychron", "Periféricos", "K2-V2", "4895248812345");
        assertNotNull(createdProduct.getId());
        assertEquals(1, createdProduct.getIdentifiers().size());
        assertThrows(DuplicateEntityException.class,
                () -> service.createProduct("Teclado Mecánico Keychron K2", "Keychron", "Periféricos", "K2-V2", null));

        Product updatedProduct = service.updateProduct(createdProduct.getId(), "Teclado Keychron K2 Pro", "K2-PRO");
        assertEquals("Teclado Keychron K2 Pro", updatedProduct.getName());

        // Registrar un nuevo usuario
        User newUser = service.registerUser("nuevo@upc.edu.co", "Segura2026!", "Estudiante UPC");
        assertNotNull(newUser.getId());

        // Recargar en una instancia nueva desde los archivos JSON del disco
        ConsoleCatalogService reloaded = new ConsoleCatalogService(tempDir.toString(), encoder);
        assertEquals(6, reloaded.listStores().size());
        assertEquals(6, reloaded.listProducts().size());
        assertEquals(3, reloaded.listUsers().size());
        assertTrue(reloaded.authenticate("nuevo@upc.edu.co", "Segura2026!").isPresent());
        assertTrue(reloaded.listProducts().stream().anyMatch(p -> "Teclado Keychron K2 Pro".equals(p.getName())));

        // Eliminar tienda y producto y verificar inexistencia
        reloaded.deleteStore(createdStore.getId());
        reloaded.deleteProduct(createdProduct.getId());
        assertEquals(5, reloaded.listStores().size());
        assertEquals(5, reloaded.listProducts().size());
        assertThrows(EntityNotFoundException.class, () -> reloaded.deleteStore(9999L));
        assertThrows(EntityNotFoundException.class, () -> reloaded.deleteProduct(9999L));
    }
}
