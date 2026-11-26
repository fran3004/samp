package co.edu.upc.samp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main entry point for SAMP (Sistema Automatizado de Mejores Precios).
 *
 * <p>Supports three execution profiles from the same artifact:
 * <ul>
 *   <li>Default ({@code web}): Spring Boot + Thymeleaf + PostgreSQL/Supabase.</li>
 *   <li>{@code scraper}: headless scheduled ingestion writing to Supabase.</li>
 *   <li>{@code console}: interactive CLI backed by {@code ArrayList} and JSON files.</li>
 * </ul>
 */
@SpringBootApplication
@EnableCaching
public class SampApplication {

    /**
     * Launches the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SampApplication.class, args);
    }
}
