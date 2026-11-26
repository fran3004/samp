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

/**
 * Elemento individual incluido dentro de una configuración armada ({@link Build}).
 */
@Entity
@Table(name = "build_items")
public class BuildItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_id", nullable = false)
    private Build build;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "price_at_save", nullable = false, precision = 14, scale = 2)
    private BigDecimal priceAtSave;

    @Column(nullable = false)
    private int quantity = 1;

    protected BuildItem() {
    }

    /**
     * Crea un ítem de configuración.
     *
     * @param build       configuración padre
     * @param product     producto seleccionado
     * @param offerId     oferta vigente opcional
     * @param priceAtSave precio guardado en el momento de agregarlo
     * @param quantity    cantidad positiva
     */
    public BuildItem(Build build, Product product, Long offerId, BigDecimal priceAtSave, int quantity) {
        this.build = build;
        this.product = product;
        this.offerId = offerId;
        this.priceAtSave = priceAtSave;
        this.quantity = Math.max(1, quantity);
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Long getOfferId() {
        return offerId;
    }

    public BigDecimal getPriceAtSave() {
        return priceAtSave;
    }

    public int getQuantity() {
        return quantity;
    }
}
