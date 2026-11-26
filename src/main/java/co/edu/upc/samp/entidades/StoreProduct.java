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
import java.time.Instant;

/**
 * Publicación específica de un producto dentro de una tienda concreta.
 * Distingue entre equivalencia verificable ({@code product}) y similitud ({@code similarToProduct}).
 */
@Entity
@Table(name = "store_products")
public class StoreProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_to_product_id")
    private Product similarToProduct;

    @Column(name = "external_id", nullable = false, length = 160)
    private String externalId;

    @Column(nullable = false, length = 350)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "match_level")
    private Short matchLevel;

    @Column(name = "match_confidence", precision = 5, scale = 4)
    private BigDecimal matchConfidence;

    @Column(name = "match_method", length = 40)
    private String matchMethod;

    @Column(name = "brand_raw", length = 120)
    private String brandRaw;

    @Column(name = "model_raw", length = 140)
    private String modelRaw;

    @Column(name = "gtin_raw", length = 60)
    private String gtinRaw;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;

    protected StoreProduct() {
    }

    /**
     * Crea una publicación de tienda.
     *
     * @param store      tienda origen
     * @param externalId identificador externo en la tienda
     * @param title      título publicado
     * @param url        enlace directo al producto
     */
    public StoreProduct(Store store, String externalId, String title, String url) {
        this.store = store;
        this.externalId = externalId;
        this.title = title;
        this.url = url;
        this.firstSeenAt = Instant.now();
        this.lastSeenAt = Instant.now();
    }

    /**
     * Vincula la publicación a un producto canónico mediante una regla verificable.
     *
     * @param product    producto canónico equivalente
     * @param level      nivel de emparejamiento (1=GTIN, 2=MPN, 3=atributos)
     * @param confidence confianza entre 0 y 1
     * @param method     descripción del método aplicado
     */
    public void linkTo(Product product, short level, BigDecimal confidence, String method) {
        this.product = product;
        this.matchLevel = level;
        this.matchConfidence = confidence;
        this.matchMethod = method;
        this.lastSeenAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Store getStore() {
        return store;
    }

    public Product getProduct() {
        return product;
    }

    public Product getSimilarToProduct() {
        return similarToProduct;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public Short getMatchLevel() {
        return matchLevel;
    }

    public BigDecimal getMatchConfidence() {
        return matchConfidence;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public String getGtinRaw() {
        return gtinRaw;
    }

    public void setGtinRaw(String gtinRaw) {
        this.gtinRaw = gtinRaw;
    }

    public boolean isActive() {
        return active;
    }
}
