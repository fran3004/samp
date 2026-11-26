package co.edu.upc.samp.servicios.seguridad;

import co.edu.upc.samp.entidades.Role;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.DuplicateEntityException;
import co.edu.upc.samp.entidades.excepciones.ValidationException;
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
 * <p>Valida estrictamente formato de correo, longitud mínima de contraseña y duplicados,
 * devolviendo mensajes claros en español ante cualquier error.
 */
@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final PasswordEncoder passwordEncoder;
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private long nextId = 1L;

    /**
     * Inicializa el servicio de autenticación y siembra cuentas de demostración
     * para los roles {@code ADMIN} y {@code REGISTERED}.
     *
     * @param passwordEncoder codificador BCrypt de contraseñas
     */
    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        seedDefaultAccounts();
    }

    private void seedDefaultAccounts() {
        registerInternal("admin@samp.co", "AdminSamp2026!", "Administrador SAMP", List.of("ADMIN", "REGISTERED"));
        registerInternal("comprador@samp.co", "Comprador2026!", "Comprador Colombia", List.of("REGISTERED"));
    }

    /**
     * Registra un nuevo usuario validando correo, nombre y contraseña segura.
     *
     * @param email       correo electrónico único
     * @param rawPassword contraseña en texto plano (mínimo 8 caracteres)
     * @param displayName nombre visible del usuario
     * @return usuario creado con rol {@code REGISTERED}
     */
    public User register(String email, String rawPassword, String displayName) {
        validateRegistrationInput(email, rawPassword, displayName);
        String normalizedEmail = email.trim().toLowerCase();
        if (usersByEmail.containsKey(normalizedEmail)) {
            throw new DuplicateEntityException("usuario", "correo electrónico", normalizedEmail);
        }
        return registerInternal(normalizedEmail, rawPassword, displayName.trim(), List.of("REGISTERED"));
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
        User user = usersByEmail.get(email.trim().toLowerCase());
        if (user == null || !user.isEnabled()) {
            return Optional.empty();
        }
        return passwordEncoder.matches(rawPassword, user.getPasswordHash())
                ? Optional.of(user)
                : Optional.empty();
    }

    /**
     * Busca un usuario registrado por su correo electrónico.
     *
     * @param email correo del usuario
     * @return usuario encontrado o vacío
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByEmail.get(email.trim().toLowerCase()));
    }

    /**
     * Lista todos los usuarios registrados para el panel de administración.
     *
     * @return lista de usuarios
     */
    public List<User> findAllUsers() {
        return new ArrayList<>(usersByEmail.values());
    }

    private synchronized User registerInternal(String email, String rawPassword, String displayName, List<String> roles) {
        User user = new User(email, passwordEncoder.encode(rawPassword), displayName);
        user.setId(nextId++);
        for (String roleName : roles) {
            user.addRole(new Role(roleName));
        }
        usersByEmail.put(user.getEmail(), user);
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
