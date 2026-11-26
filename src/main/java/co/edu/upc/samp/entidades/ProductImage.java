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

/**
 * Referencia por URL a una imagen de producto (no se almacenan archivos binarios locales).
 */
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private int position;

    protected ProductImage() {
    }

    /**
     * Crea una referencia de imagen ordenada.
     *
     * @param product  producto asociado
     * @param url      URL de la imagen
     * @param position posición de visualización
     */
    public ProductImage(Product product, String url, int position) {
        this.product = product;
        this.url = url;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int getPosition() {
        return position;
    }
}
