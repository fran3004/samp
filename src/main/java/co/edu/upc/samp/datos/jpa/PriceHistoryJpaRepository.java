package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la tabla {@code price_history}.
 */
public interface PriceHistoryJpaRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByStoreProductProductIdOrderByRecordedAtAsc(Long productId);
}
