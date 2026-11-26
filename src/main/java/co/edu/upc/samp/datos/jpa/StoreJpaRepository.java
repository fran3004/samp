package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la tabla {@code stores}.
 */
public interface StoreJpaRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByCodeIgnoreCase(String code);

    List<Store> findByActiveTrueOrderByNameAsc();
}
