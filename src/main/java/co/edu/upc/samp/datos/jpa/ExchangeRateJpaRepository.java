package co.edu.upc.samp.datos.jpa;

import co.edu.upc.samp.entidades.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la tabla {@code exchange_rates}.
 */
public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findFirstByBaseCurrencyAndQuoteCurrencyOrderByFetchedAtDesc(
            String baseCurrency, String quoteCurrency);
}
