package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la tabla {@code offers}.
 */
public interface OfferJpaRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByStoreProductProductIdAndCurrentTrueOrderByFinalPriceRefAsc(Long productId);

    List<Offer> findByCurrentTrueOrderByFinalPriceRefAsc();
}
