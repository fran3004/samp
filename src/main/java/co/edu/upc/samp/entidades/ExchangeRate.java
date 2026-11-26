package co.edu.upc.samp.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tasa de cambio fechada entre dos monedas ISO 4217 para garantizar trazabilidad de conversión.
 */
@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    protected ExchangeRate() {
    }

    /**
     * Crea una tasa de cambio fechada.
     *
     * @param baseCurrency  moneda origen
     * @param quoteCurrency moneda destino
     * @param rate          factor multiplicador
     * @param source        fuente de la tasa
     */
    public ExchangeRate(String baseCurrency, String quoteCurrency, BigDecimal rate, String source) {
        this.baseCurrency = baseCurrency.trim().toUpperCase();
        this.quoteCurrency = quoteCurrency.trim().toUpperCase();
        this.rate = rate;
        this.source = source;
        this.fetchedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getSource() {
        return source;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
