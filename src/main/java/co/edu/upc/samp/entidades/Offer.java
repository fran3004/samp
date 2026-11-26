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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Oferta de precio observada en un instante concreto para una publicación de tienda.
 *
 * <p>Aplica el principio de transparencia de SAMP: un envío o impuesto desconocido
 * nunca se asume como cero y se señala como total estimado incompleto.
 */
@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_product_id", nullable = false)
    private StoreProduct storeProduct;

    @Column(name = "price_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "shipping_amount", precision = 14, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "shipping_known", nullable = false)
    private boolean shippingKnown;

    @Column(name = "tax_amount", precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "tax_known", nullable = false)
    private boolean taxKnown;

    @Column(name = "final_price_ref", nullable = false, precision = 14, scale = 2)
    private BigDecimal finalPriceRef;

    @Column(nullable = false, length = 40)
    private String availability = "IN_STOCK";

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt = Instant.now();

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    protected Offer() {
    }

    /**
     * Crea una oferta transparente con precio publicado, envío e impuestos conocidos.
     *
     * @param storeProduct   publicación asociada
     * @param priceAmount    precio publicado en la moneda original
     * @param currencyCode   moneda original ISO 4217
     * @param shippingAmount costo de envío (nulo si es desconocido)
     * @param shippingKnown  indica si el costo de envío es conocido
     * @param taxAmount      impuestos conocidos (nulo si son desconocidos)
     * @param taxKnown       indica si los impuestos son conocidos
     * @param finalPriceRef  precio total estimado en moneda de referencia (USD)
     * @param availability   disponibilidad reportada
     */
    public Offer(
            StoreProduct storeProduct,
            BigDecimal priceAmount,
            String currencyCode,
            BigDecimal shippingAmount,
            boolean shippingKnown,
            BigDecimal taxAmount,
            boolean taxKnown,
            BigDecimal finalPriceRef,
            String availability
    ) {
        this.storeProduct = storeProduct;
        this.priceAmount = priceAmount;
        this.currencyCode = currencyCode.trim().toUpperCase();
        this.shippingAmount = shippingKnown ? shippingAmount : null;
        this.shippingKnown = shippingKnown;
        this.taxAmount = taxKnown ? taxAmount : null;
        this.taxKnown = taxKnown;
        this.finalPriceRef = finalPriceRef;
        this.availability = availability == null ? "UNKNOWN" : availability;
        this.sourceUrl = storeProduct != null ? storeProduct.getUrl() : null;
        this.checkedAt = Instant.now();
        this.current = true;
    }

    /**
     * Calcula el total en la moneda original sumando únicamente los componentes conocidos.
     *
     * @return monto total conocido en la moneda original
     */
    public Money knownTotalInOriginalCurrency() {
        BigDecimal total = priceAmount;
        if (shippingKnown && shippingAmount != null) {
            total = total.add(shippingAmount);
        }
        if (taxKnown && taxAmount != null) {
            total = total.add(taxAmount);
        }
        return new Money(total, currencyCode);
    }

    /**
     * Indica si la oferta conoce tanto el envío como los impuestos, permitiendo llamarla
     * «total estimado completo».
     *
     * @return {@code true} si no faltan costos por conocer
     */
    public boolean isCompleteTotal() {
        return shippingKnown && taxKnown;
    }

    /**
     * Determina si la oferta está desactualizada respecto al tiempo de frescura permitido.
     *
     * @param ttl duración máxima de frescura
     * @return {@code true} si la observación supera el TTL
     */
    public boolean isStale(Duration ttl) {
        return Duration.between(checkedAt, Instant.now()).compareTo(ttl) > 0;
    }

    /**
     * Marca la oferta actual como reemplazada por una nueva observación.
     */
    public void markSuperseded() {
        this.current = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StoreProduct getStoreProduct() {
        return storeProduct;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public boolean isShippingKnown() {
        return shippingKnown;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public boolean isTaxKnown() {
        return taxKnown;
    }

    public BigDecimal getFinalPriceRef() {
        return finalPriceRef;
    }

    public String getAvailability() {
        return availability;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public boolean isCurrent() {
        return current;
    }
}
