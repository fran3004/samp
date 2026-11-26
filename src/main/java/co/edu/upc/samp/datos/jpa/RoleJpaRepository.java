package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la tabla {@code roles}.
 */
public interface RoleJpaRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByNameIgnoreCase(String name);
}
