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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Configuración y estado de salud (circuit breaker) de un conector asignado a una tienda.
 * Encapsula la lógica de fallos consecutivos, apertura del disyuntor y tasa de éxito.
 */
@Entity
@Table(name = "store_connectors")
public class StoreConnectorConfig {

    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration HALF_OPEN_COOLDOWN = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "connector_type", nullable = false, length = 120)
    private String connectorType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectorKind kind;

    @Column(nullable = false)
    private int priority = 10;

    @Column(name = "success_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal successRate = BigDecimal.ONE;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "circuit_state", nullable = false, length = 15)
    private CircuitState circuitState = CircuitState.CLOSED;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "robots_checked_at")
    private Instant robotsCheckedAt;

    @Column(name = "ttl_minutes", nullable = false)
    private int ttlMinutes = 1440;

    protected StoreConnectorConfig() {
    }

    /**
     * Crea una configuración de conector para una tienda.
     *
     * @param store         tienda asociada
     * @param connectorType nombre de la clase del conector
     * @param kind          tipo de conector
     * @param priority      prioridad (menor valor se intenta primero)
     */
    public StoreConnectorConfig(Store store, String connectorType, ConnectorKind kind, int priority) {
        this.store = store;
        this.connectorType = connectorType;
        this.kind = kind;
        this.priority = priority;
    }

    /**
     * Indica si el conector puede ejecutarse en este instante según su estado y disyuntor.
     *
     * @return {@code true} si está habilitado y su circuito no está abierto
     */
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        if (circuitState == CircuitState.CLOSED || circuitState == CircuitState.HALF_OPEN) {
            return true;
        }
        return lastSuccessAt != null
                && Duration.between(lastSuccessAt, Instant.now()).compareTo(HALF_OPEN_COOLDOWN) >= 0;
    }

    /**
     * Registra una ejecución exitosa, cierra el circuito y mejora la tasa de éxito.
     */
    public void recordSuccess() {
        this.consecutiveFailures = 0;
        this.circuitState = CircuitState.CLOSED;
        this.lastSuccessAt = Instant.now();
        this.successRate = this.successRate.multiply(new BigDecimal("0.8"))
                .add(new BigDecimal("0.2"));
    }

    /**
     * Registra un fallo de ejecución; abre el circuito al alcanzar 3 fallos consecutivos.
     */
    public void recordFailure() {
        this.consecutiveFailures++;
        this.successRate = this.successRate.multiply(new BigDecimal("0.8"));
        if (this.consecutiveFailures >= FAILURE_THRESHOLD) {
            this.circuitState = CircuitState.OPEN;
        }
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ConnectorKind getKind() {
        return kind;
    }

    public int getPriority() {
        return priority;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public CircuitState getCircuitState() {
        return circuitState;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }
}
