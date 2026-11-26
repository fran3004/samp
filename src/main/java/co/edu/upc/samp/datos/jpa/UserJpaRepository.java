package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la tabla {@code users}.
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
