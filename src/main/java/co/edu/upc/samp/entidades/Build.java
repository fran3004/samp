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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración o lista armada por un usuario con apoyo del asistente de IA general
 * (setup, PC, kit de estudio, regalos o compras con presupuesto en cualquier categoría).
 */
@Entity
@Table(name = "builds")
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String category = "GENERAL";

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, length = 3)
    private String currency = "COP";

    @Column(length = 200)
    private String purpose;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "total_final_ref", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalFinalRef = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BuildItem> items = new ArrayList<>();

    protected Build() {
    }

    /**
     * Crea una configuración general con presupuesto y categoría libre.
     *
     * @param user     usuario dueño de la configuración
     * @param name     nombre descriptivo
     * @param category categoría (GENERAL, PC, HOGAR, GAMING, etc.)
     * @param budget   presupuesto máximo
     * @param currency moneda del presupuesto
     * @param purpose  propósito de compra
     */
    public Build(User user, String name, String category, BigDecimal budget, String currency, String purpose) {
        this.user = user;
        this.name = name.trim();
        this.category = (category == null || category.isBlank()) ? "GENERAL" : category.trim().toUpperCase();
        this.budget = budget;
        this.currency = currency.trim().toUpperCase();
        this.purpose = purpose;
        this.createdAt = Instant.now();
    }

    /**
     * Agrega un artículo a la configuración y recalcula el total acumulado.
     *
     * @param item elemento a incluir
     */
    public void addItem(BuildItem item) {
        items.add(item);
        recalculateTotal();
    }

    /**
     * Recalcula el total de la configuración sumando el precio por cantidad de cada ítem.
     */
    public void recalculateTotal() {
        this.totalFinalRef = items.stream()
                .map(i -> i.getPriceAtSave().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalFinalRef() {
        return totalFinalRef;
    }

    public List<BuildItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
