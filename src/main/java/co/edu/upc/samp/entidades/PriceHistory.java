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
 * Registro histórico de precio observado para análisis de tendencias (mínimo 30 días para RF-22).
 */
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_product_id", nullable = false)
    private StoreProduct storeProduct;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(precision = 14, scale = 2)
    private BigDecimal shipping;

    @Column(name = "final_price_ref", nullable = false, precision = 14, scale = 2)
    private BigDecimal finalPriceRef;

    @Column(nullable = false, length = 40)
    private String availability = "IN_STOCK";

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    protected PriceHistory() {
    }

    /**
     * Crea una entrada histórica a partir de una oferta observada.
     *
     * @param offer      oferta registrada
     * @param recordedAt instante de la observación
     */
    public PriceHistory(Offer offer, Instant recordedAt) {
        this.offer = offer;
        this.storeProduct = offer.getStoreProduct();
        this.price = offer.getPriceAmount();
        this.currency = offer.getCurrencyCode();
        this.shipping = offer.getShippingAmount();
        this.finalPriceRef = offer.getFinalPriceRef();
        this.availability = offer.getAvailability();
        this.recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getShipping() {
        return shipping;
    }

    public BigDecimal getFinalPriceRef() {
        return finalPriceRef;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
