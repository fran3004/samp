package co.edu.upc.samp.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Tienda en línea monitoreada por SAMP (Mercado Libre, Amazon, Temu, Falabella o AliExpress).
 * Incluye marca transparente de origen de datos ({@code REAL} o {@code DEMO}).
 */
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "reliability_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal reliabilityScore = new BigDecimal("0.85");

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_kind", nullable = false, length = 10)
    private DataKind dataKind = DataKind.DEMO;

    @Column(name = "shipping_policy_type", nullable = false, length = 40)
    private String shippingPolicyType = "UNKNOWN";

    @Column(name = "flat_shipping_amount", precision = 14, scale = 2)
    private BigDecimal flatShippingAmount;

    @Column(name = "tax_included", nullable = false)
    private boolean taxIncluded = true;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    protected Store() {
    }

    /**
     * Crea una tienda con sus parámetros principales.
     *
     * @param code            código único interno
     * @param countryCode     país ISO 3166-1 alfa-2
     * @param name            nombre visible de la tienda
     * @param baseUrl         URL base de la tienda
     * @param defaultCurrency moneda por defecto
     * @param dataKind        origen de datos (REAL o DEMO)
     */
    public Store(String code, String countryCode, String name, String baseUrl, String defaultCurrency, DataKind dataKind) {
        this.code = code.trim().toUpperCase();
        this.countryCode = countryCode.trim().toUpperCase();
        this.name = name.trim();
        this.baseUrl = baseUrl.trim();
        this.defaultCurrency = defaultCurrency.trim().toUpperCase();
        this.dataKind = dataKind == null ? DataKind.DEMO : dataKind;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public BigDecimal getReliabilityScore() {
        return reliabilityScore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public DataKind getDataKind() {
        return dataKind;
    }

    public void setDataKind(DataKind dataKind) {
        this.dataKind = dataKind;
    }

    public String getShippingPolicyType() {
        return shippingPolicyType;
    }

    public BigDecimal getFlatShippingAmount() {
        return flatShippingAmount;
    }

    public boolean isTaxIncluded() {
        return taxIncluded;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }
}
