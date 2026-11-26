package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la tabla {@code products}.
 */
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.identifiers i WHERE UPPER(i.value) = UPPER(:gtin)")
    Optional<Product> findByIdentifierValue(@Param("gtin") String gtin);
}
