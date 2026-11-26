package co.edu.upc.samp.entidades;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Objeto de valor inmutable que representa un monto monetario con su código de moneda ISO 4217.
 *
 * <p>Demuestra encapsulamiento: valida entradas en construcción, evita mutación externa
 * y concentra las operaciones aritméticas y de conversión de moneda.
 */
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    /**
     * Crea una instancia inmutable de {@code Money}.
     *
     * @param amount   monto no negativo
     * @param currency código ISO 4217 de 3 letras (por ejemplo, {@code COP} o {@code USD})
     */
    public Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto monetario debe ser mayor o igual a cero.");
        }
        if (currency == null || currency.trim().length() != 3) {
            throw new IllegalArgumentException("El código de moneda debe tener exactamente 3 caracteres ISO.");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.trim().toUpperCase();
    }

    /**
     * Crea una instancia de {@code Money} a partir de un valor {@code double} (sobrecarga de fábrica).
     *
     * @param amount   monto numérico
     * @param currency código ISO 4217
     * @return nueva instancia de {@code Money}
     */
    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Crea una instancia de {@code Money} con valor cero para la moneda indicada.
     *
     * @param currency código ISO 4217
     * @return monto cero en la moneda dada
     */
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Suma dos montos de la misma moneda.
     *
     * @param other otro monto en la misma moneda
     * @return nuevo {@code Money} con la suma
     */
    public Money plus(Money other) {
        Objects.requireNonNull(other, "El monto a sumar no puede ser nulo.");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "No se pueden sumar monedas distintas sin conversión previa: "
                            + this.currency + " y " + other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Multiplica el monto por una cantidad entera positiva.
     *
     * @param quantity cantidad de unidades
     * @return nuevo {@code Money} escalado
     */
    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    /**
     * Convierte el monto actual a otra moneda aplicando la tasa de cambio suministrada.
     *
     * @param targetCurrency código de moneda destino
     * @param rate           tasa de conversión positiva
     * @return nuevo {@code Money} en la moneda destino
     */
    public Money convertTo(String targetCurrency, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La tasa de cambio debe ser mayor que cero.");
        }
        if (this.currency.equalsIgnoreCase(targetCurrency)) {
            return this;
        }
        return new Money(this.amount.multiply(rate), targetCurrency);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
