package co.edu.upc.samp.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa un rol del sistema para control de acceso (VISITOR, REGISTERED, ADMIN).
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    protected Role() {
    }

    /**
     * Crea un rol con el nombre normalizado en mayúsculas.
     *
     * @param name nombre del rol
     */
    public Role(String name) {
        this.name = name.trim().toUpperCase();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
