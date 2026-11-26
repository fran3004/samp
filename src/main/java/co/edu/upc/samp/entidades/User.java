package co.edu.upc.samp.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad de usuario del sistema con control de acceso por roles y borrado lógico.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "CO";

    @Column(nullable = false, length = 10)
    private String locale = "es-CO";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

    /**
     * Crea un usuario registrado para Colombia con idioma español por defecto.
     *
     * @param email        correo electrónico único
     * @param passwordHash contraseña cifrada con BCrypt
     * @param displayName  nombre visible del usuario
     */
    public User(String email, String passwordHash, String displayName) {
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName.trim();
        this.countryCode = "CO";
        this.locale = "es-CO";
        this.enabled = true;
        this.createdAt = Instant.now();
    }

    /**
     * Verifica si el usuario posee el rol indicado.
     *
     * @param roleName nombre del rol a comprobar
     * @return {@code true} si el rol está asignado
     */
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Asigna un rol al usuario.
     *
     * @param role rol a agregar
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Desactiva la cuenta del usuario de forma segura.
     */
    public void disable() {
        this.enabled = false;
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
