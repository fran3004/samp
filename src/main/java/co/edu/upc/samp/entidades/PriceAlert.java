package co.edu.upc.samp.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Alerta de precio objetivo configurada por un usuario sobre un producto o una configuración armada.
 */
@Entity
@Table(name = "price_alerts")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "build_id")
    private Long buildId;

    @Column(name = "target_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal targetPrice;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "COP";

    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PriceAlert() {
    }

    /**
     * Crea una alerta de precio para un producto.
     *
     * @param user        usuario propietario
     * @param product     producto vigilado
     * @param targetPrice precio objetivo
     * @param currency    moneda del objetivo
     */
    public PriceAlert(User user, Product product, BigDecimal targetPrice, String currency) {
        this.user = user;
        this.product = product;
        this.targetPrice = targetPrice;
        this.currency = currency.trim().toUpperCase();
        this.createdAt = Instant.now();
    }

    /**
     * Evalúa si un precio observado en la misma moneda alcanza o mejora el objetivo.
     *
     * @param observedPrice precio observado
     * @return {@code true} si debe dispararse la alerta
     */
    public boolean shouldTrigger(BigDecimal observedPrice) {
        return !notified && observedPrice != null && observedPrice.compareTo(targetPrice) <= 0;
    }

    /**
     * Marca la alerta como notificada para evitar envíos duplicados.
     */
    public void markNotified() {
        this.notified = true;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }

    public Long getBuildId() {
        return buildId;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isNotified() {
        return notified;
    }
}
