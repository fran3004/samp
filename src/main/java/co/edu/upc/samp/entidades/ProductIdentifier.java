package co.edu.upc.samp.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Identificador verificable (GTIN, EAN, UPC, ISBN o MPN) asociado a un producto canónico.
 */
@Entity
@Table(name = "product_identifiers")
public class ProductIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdentifierType type;

    @Column(nullable = false, length = 80)
    private String value;

    protected ProductIdentifier() {
    }

    /**
     * Crea un identificador para un producto.
     *
     * @param product producto dueño del identificador
     * @param type    tipo de identificador
     * @param value   código normalizado
     */
    public ProductIdentifier(Product product, IdentifierType type, String value) {
        this.product = product;
        this.type = type;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public IdentifierType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
