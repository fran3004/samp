package co.edu.upc.samp.servicios.seguridad;

import co.edu.upc.samp.datos.jpa.RoleJpaRepository;
import co.edu.upc.samp.datos.jpa.UserJpaRepository;
import co.edu.upc.samp.entidades.Role;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import co.edu.upc.samp.entidades.excepciones.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Servicio de autenticación, registro y control de acceso por roles ({@code VISITOR}, {@code REGISTERED}, {@code ADMIN}).
 *
 * <p>Cuando existe conexión activa a PostgreSQL/Supabase, lee y persiste usuarios directamente
 * en las tablas {@code users}, {@code roles} y {@code user_roles} mediante JPA; en modo sin
 * base de datos opera de forma transparente sobre cuentas de demostración en memoria.
 */
@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final Map<String, User> fallbackUsersByEmail = new ConcurrentHashMap<>();
    private long nextId = 1L;

    /**
     * Constructor principal con repositorios JPA opcionales (activos cuando hay Supabase/Postgres).
     *
     * @param passwordEncoder   codificador BCrypt
     * @param userJpaRepository repositorio JPA de usuarios (opcional)
     * @param roleJpaRepository repositorio JPA de roles (opcional)
     */
    @Autowired
    public AuthService(
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) UserJpaRepository userJpaRepository,
            @Autowired(required = false) RoleJpaRepository roleJpaRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        seedFallbackAccounts();
    }

    /**
     * Sobrecarga de constructor para pruebas unitarias en memoria sin contexto JPA.
     *
     * @param passwordEncoder codificador BCrypt
     */
    public AuthService(PasswordEncoder passwordEncoder) {
        this(passwordEncoder, null, null);
    }

    private void seedFallbackAccounts() {
        registerInMemory("admin@samp.co", "AdminSamp2026!", "Administrador SAMP", List.of("ADMIN", "REGISTERED"));
        registerInMemory("comprador@samp.co", "Comprador2026!", "Comprador Colombia", List.of("REGISTERED"));
    }

    /**
     * Registra un nuevo usuario validando correo, nombre y contraseña segura.
     * Persiste en Supabase/PostgreSQL si JPA está activo.
     *
     * @param email       correo electrónico único
     * @param rawPassword contraseña en texto plano (mínimo 8 caracteres)
     * @param displayName nombre visible del usuario
     * @return usuario creado con rol {@code REGISTERED}
     */
    public User register(String email, String rawPassword, String displayName) {
        validateRegistrationInput(email, rawPassword, displayName);
        String normalizedEmail = email.trim().toLowerCase();

        if (userJpaRepository != null && roleJpaRepository != null) {
            if (userJpaRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new DuplicateEntityException("usuario", "correo electrónico", normalizedEmail);
            }
            User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword), displayName.trim());
            Role registered = roleJpaRepository.findByNameIgnoreCase("REGISTERED")
                    .orElseGet(() -> roleJpaRepository.save(new Role("REGISTERED")));
            user.addRole(registered);
            return userJpaRepository.save(user);
        }

        if (fallbackUsersByEmail.containsKey(normalizedEmail)) {
            throw new DuplicateEntityException("usuario", "correo electrónico", normalizedEmail);
        }
        return registerInMemory(normalizedEmail, rawPassword, displayName.trim(), List.of("REGISTERED"));
    }

    /**
     * Autentica un usuario comparando su correo y contraseña cifrada con BCrypt.
     *
     * @param email       correo ingresado
     * @param rawPassword contraseña ingresada
     * @return usuario autenticado o vacío si las credenciales son incorrectas
     */
    public Optional<User> authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return Optional.empty();
        }
        return findByEmail(email)
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }

    /**
     * Busca un usuario registrado por su correo electrónico (Supabase/JPA o respaldo en memoria).
     *
     * @param email correo del usuario
     * @return usuario encontrado o vacío
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalized = email.trim().toLowerCase();
        if (userJpaRepository != null) {
            return userJpaRepository.findByEmailIgnoreCase(normalized);
        }
        return Optional.ofNullable(fallbackUsersByEmail.get(normalized));
    }

    /**
     * Lista todos los usuarios registrados para el panel de administración.
     *
     * @return lista de usuarios
     */
    public List<User> findAllUsers() {
        if (userJpaRepository != null) {
            return userJpaRepository.findAll();
        }
        return new ArrayList<>(fallbackUsersByEmail.values());
    }

    /**
     * Indica si el servicio está operando contra PostgreSQL/Supabase real.
     *
     * @return {@code true} si JPA está conectado
     */
    public boolean isDatabaseBacked() {
        return userJpaRepository != null;
    }

    private synchronized User registerInMemory(String email, String rawPassword, String displayName, List<String> roles) {
        User user = new User(email, passwordEncoder.encode(rawPassword), displayName);
        user.setId(nextId++);
        for (String roleName : roles) {
            user.addRole(new Role(roleName));
        }
        fallbackUsersByEmail.put(user.getEmail(), user);
        return user;
    }

    private void validateRegistrationInput(String email, String rawPassword, String displayName) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("El correo electrónico ingresado no tiene un formato válido.");
        }
        if (displayName == null || displayName.trim().length() < 2) {
            throw new ValidationException("El nombre visible debe tener al menos 2 caracteres.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres.");
        }
    }
}
