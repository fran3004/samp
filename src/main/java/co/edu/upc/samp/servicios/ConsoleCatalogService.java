package co.edu.upc.samp.servicios;

import co.edu.upc.samp.datos.archivo.JsonProductRepository;
import co.edu.upc.samp.datos.archivo.JsonStoreRepository;
import co.edu.upc.samp.datos.archivo.JsonUserRepository;
import co.edu.upc.samp.entidades.Brand;
import co.edu.upc.samp.entidades.Category;
import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.IdentifierType;
import co.edu.upc.samp.entidades.Product;
import co.edu.upc.samp.entidades.Role;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.EntityNotFoundException;
import co.edu.upc.samp.entidades.excepciones.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de persistencia en archivos JSON respaldados por {@code ArrayList} para el modo consola.
 *
 * <p>Implementa CRUD completo y consistente sobre las tres entidades principales ({@code Store},
 * {@code Product} y {@code User}), manteniendo intacta la arquitectura de 4 capas.
 */
@Service
@Profile({"consola", "console"})
public class ConsoleCatalogService {

    private final JsonStoreRepository storeRepository;
    private final JsonProductRepository productRepository;
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
        this.productRepository = new JsonProductRepository(basePath);
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
        if (productRepository.findAll().isEmpty()) {
            createProduct("Audífonos Sony WH-1000XM5", "Sony", "Audio", "WH-1000XM5", "0027242923232");
            createProduct("Mouse Gamer Logitech G502 HERO", "Logitech", "Periféricos", "G502 HERO", "0097855141224");
            createProduct("Freidora de Aire Ninja 5.2L", "Ninja", "Hogar", "AF161", "0622356563093");
            createProduct("Hub USB-C 7 en 1 HDMI 4K", "Ugreen", "Accesorios", "CM478", "6901234567890");
            createProduct("Crema de Avellanas Nutella 350g", "Ferrero", "Alimentos", "Nutella 350g", "3017620422003");
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
     * Registra un nuevo usuario en {@code users.json} validando formato y unicidad.
     *
     * @param email       correo único
     * @param rawPassword contraseña mínimo 8 caracteres
     * @param displayName nombre visible
     * @return usuario creado
     */
    public User registerUser(String email, String rawPassword, String displayName) {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new ValidationException("El correo electrónico no tiene un formato válido.");
        }
        if (displayName == null || displayName.trim().length() < 2) {
            throw new ValidationException("El nombre debe tener al menos 2 caracteres.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres.");
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), displayName);
        user.addRole(new Role("REGISTERED"));
        return userRepository.save(user);
    }

    /**
     * Lista todos los usuarios guardados en {@code users.json}.
     *
     * @return lista de usuarios
     */
    public List<User> listUsers() {
        return userRepository.findAll();
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
     * @return tienda actualizada
     */
    public Store updateStoreDataKind(Long id, DataKind kind) {
        Store existing = storeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("tienda", id));
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

    /**
     * Lista todos los productos canónicos almacenados en {@code products.json}.
     *
     * @return lista de productos
     */
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    /**
     * Crea un producto canónico en {@code products.json} con GTIN opcional.
     *
     * @param name         nombre comercial
     * @param brandName    marca
     * @param categoryName categoría
     * @param model        modelo o referencia
     * @param gtin         código GTIN/EAN opcional
     * @return producto persistido
     */
    public Product createProduct(String name, String brandName, String categoryName, String model, String gtin) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("El nombre del producto es obligatorio.");
        }
        if (categoryName == null || categoryName.isBlank()) {
            throw new ValidationException("La categoría del producto es obligatoria.");
        }
        Brand brand = (brandName == null || brandName.isBlank()) ? null : new Brand(brandName);
        Category category = new Category(categoryName, null);
        Product product = new Product(brand, category, name, model);
        if (gtin != null && !gtin.isBlank()) {
            product.addIdentifier(IdentifierType.GTIN, gtin);
        }
        return productRepository.save(product);
    }

    /**
     * Actualiza nombre y modelo de un producto existente en {@code products.json}.
     *
     * @param id       identificador del producto
     * @param newName  nuevo nombre comercial
     * @param newModel nuevo modelo
     * @return producto actualizado
     */
    public Product updateProduct(Long id, String newName, String newModel) {
        if (newName == null || newName.isBlank()) {
            throw new ValidationException("El nuevo nombre del producto no puede estar vacío.");
        }
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("producto", id));
        existing.setName(newName.trim());
        existing.setModel(newModel == null ? null : newModel.trim());
        return productRepository.update(id, existing);
    }

    /**
     * Elimina un producto canónico de {@code products.json}.
     *
     * @param id identificador a eliminar
     */
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
