package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.StoreConnectorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la tabla {@code store_connectors}.
 */
public interface StoreConnectorJpaRepository extends JpaRepository<StoreConnectorConfig, Long> {

    List<StoreConnectorConfig> findByStoreIdOrderByPriorityAsc(Long storeId);

    List<StoreConnectorConfig> findByEnabledTrueOrderByPriorityAsc();
}
