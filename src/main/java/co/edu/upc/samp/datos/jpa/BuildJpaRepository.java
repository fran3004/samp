package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.Build;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la tabla {@code builds}.
 */
public interface BuildJpaRepository extends JpaRepository<Build, Long> {

    List<Build> findByUserIdOrderByCreatedAtDesc(Long userId);
}
