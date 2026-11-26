package co.edu.upc.samp.servicios;

import co.edu.upc.samp.datos.archivo.JsonStoreRepository;
import co.edu.upc.samp.datos.archivo.JsonUserRepository;
import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.Role;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de persistencia en archivos JSON respaldados por {@code ArrayList} para el perfil {@code consola}.
 *
 * <p>Mantiene intacta la arquitectura de 4 capas: la consola (capa {@code presentacion})
 * solo invoca este servicio y nunca accede directamente a los repositorios de {@code datos}.
 */
@Service
@Profile("consola")
public class ConsoleCatalogService {

    private final JsonStoreRepository storeRepository;
    private final JsonUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inicializa los repositorios JSON en el directorio configurado y siembra datos iniciales
     * si los archivos aún no existen.
     *
     * @param jsonDir         carpeta local de archivos JSON
     * @param passwordEncoder codificador BCrypt
     */
    public ConsoleCatalogService(
            @Value("${samp.storage.json-dir:./data-json}") String jsonDir,
            PasswordEncoder passwordEncoder
    ) {
        Path basePath = Path.of(jsonDir);
        this.storeRepository = new JsonStoreRepository(basePath);
        this.userRepository = new JsonUserRepository(basePath);
        this.passwordEncoder = passwordEncoder;
        seedDefaultsIfEmpty();
    }

    private void seedDefaultsIfEmpty() {
        if (storeRepository.findAll().isEmpty()) {
            storeRepository.save(new Store("MERCADOLIBRE_CO", "CO", "Mercado Libre Colombia", "https://www.mercadolibre.com.co", "COP", DataKind.DEMO));
            storeRepository.save(new Store("AMAZON_CO", "US", "Amazon (Envío a Colombia)", "https://www.amazon.com", "USD", DataKind.DEMO));
            storeRepository.save(new Store("TEMU_CO", "CN", "Temu Colombia", "https://www.temu.com/co", "COP", DataKind.DEMO));
            storeRepository.save(new Store("FALABELLA_CO", "CO", "Falabella Colombia", "https://www.falabella.com.co", "COP", DataKind.DEMO));
            storeRepository.save(new Store("ALIEXPRESS_CO", "CN", "AliExpress Choice", "https://es.aliexpress.com", "USD", DataKind.DEMO));
        }
        if (userRepository.findAll().isEmpty()) {
            User admin = new User("admin@samp.co", passwordEncoder.encode("AdminSamp2026!"), "Administrador SAMP");
            admin.addRole(new Role("ADMIN"));
            admin.addRole(new Role("REGISTERED"));
            userRepository.save(admin);

            User buyer = new User("comprador@samp.co", passwordEncoder.encode("Comprador2026!"), "Comprador Colombia");
            buyer.addRole(new Role("REGISTERED"));
            userRepository.save(buyer);
        }
    }

    /**
     * Autentica un usuario contra el archivo {@code users.json}.
     *
     * @param email       correo del usuario
     * @param rawPassword contraseña ingresada
     * @return usuario autenticado o vacío
     */
    public Optional<User> authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }

    /**
     * Lista todas las tiendas guardadas en {@code stores.json}.
     *
     * @return lista de tiendas
     */
    public List<Store> listStores() {
        return storeRepository.findAll();
    }

    /**
     * Crea una nueva tienda validando todos los campos obligatorios.
     *
     * @param code     código único
     * @param name     nombre comercial
     * @param url      URL base
     * @param currency código de moneda ISO de 3 letras
     * @param kind     origen de datos ({@code REAL} o {@code DEMO})
     * @return tienda persistida en {@code stores.json}
     */
    public Store createStore(String code, String name, String url, String currency, DataKind kind) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new ValidationException("El código y el nombre de la tienda son obligatorios.");
        }
        if (url == null || !url.startsWith("http")) {
            throw new ValidationException("La URL de la tienda debe comenzar por http:// o https://.");
        }
        if (currency == null || currency.trim().length() != 3) {
            throw new ValidationException("La moneda debe tener exactamente 3 letras (ej. COP o USD).");
        }
        return storeRepository.save(new Store(code, "CO", name, url, currency, kind));
    }

    /**
     * Actualiza el origen de datos ({@code REAL} o {@code DEMO}) de una tienda existente.
     *
     * @param id   identificador de la tienda
     * @param kind nuevo origen de datos
     * @return tienda actualizada en {@code stores.json}
     */
    public Store updateStoreDataKind(Long id, DataKind kind) {
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new ValidationException("No existe una tienda con ID " + id + "."));
        existing.setDataKind(kind);
        return storeRepository.update(id, existing);
    }

    /**
     * Elimina una tienda por su identificador en {@code stores.json}.
     *
     * @param id identificador a eliminar
     */
    public void deleteStore(Long id) {
        storeRepository.deleteById(id);
    }
}
