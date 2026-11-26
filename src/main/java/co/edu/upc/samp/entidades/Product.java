package co.edu.upc.samp.entidades;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Producto canónico normalizado en SAMP que agrupa publicaciones equivalentes de distintas tiendas.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 140)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductIdentifier> identifiers = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    /**
     * Crea un producto canónico con su marca, categoría, nombre y modelo.
     *
     * @param brand    marca del producto (opcional)
     * @param category categoría obligatoria
     * @param name     nombre comercial normalizado
     * @param model    referencia o modelo (opcional)
     */
    public Product(Brand brand, Category category, String name, String model) {
        this.brand = brand;
        this.category = category;
        this.name = name.trim();
        this.model = model == null ? null : model.trim();
        this.createdAt = Instant.now();
    }

    /**
     * Agrega un identificador universal (GTIN/EAN/MPN) al producto evitando duplicados.
     *
     * @param type  tipo de identificador
     * @param value valor del código
     */
    public void addIdentifier(IdentifierType type, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        boolean exists = identifiers.stream()
                .anyMatch(i -> i.getType() == type && i.getValue().equalsIgnoreCase(value.trim()));
        if (!exists) {
            identifiers.add(new ProductIdentifier(this, type, value.trim()));
        }
    }

    /**
     * Agrega una URL de imagen manteniendo el orden de presentación.
     *
     * @param url dirección URL de la imagen
     */
    public void addImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        images.add(new ProductImage(this, url.trim(), images.size()));
    }

    /**
     * Evalúa si dos productos son equivalentes por regla verificable (GTIN/EAN compartido).
     *
     * @param other otro producto canónico
     * @return {@code true} si comparten un identificador universal exacto
     */
    public boolean isEquivalentTo(Product other) {
        if (other == null) {
            return false;
        }
        return this.identifiers.stream().anyMatch(id1 ->
                other.identifiers.stream().anyMatch(id2 ->
                        id1.getType() == id2.getType() && id1.getValue().equalsIgnoreCase(id2.getValue())));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Brand getBrand() {
        return brand;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ProductIdentifier> getIdentifiers() {
        return Collections.unmodifiableList(identifiers);
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
