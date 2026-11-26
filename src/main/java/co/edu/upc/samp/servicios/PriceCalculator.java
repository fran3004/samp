package co.edu.upc.samp.servicios;

import co.edu.upc.samp.entidades.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculadora transparente de precios finales y conversiones de moneda en SAMP.
 *
 * <p>Implementa la fórmula {@code Pf = Pp + Pe + Pi}. Si el envío ({@code Pe}) o los impuestos
 * ({@code Pi}) son desconocidos, nunca los trata como cero y marca el resultado como incompleto.
 */
@Service
public class PriceCalculator {

    private static final BigDecimal DEFAULT_USD_TO_COP = new BigDecimal("4250.00");

    /**
     * Resultado inmutable del cálculo de precio final transparente.
     *
     * @param knownTotal        total sumando únicamente costos conocidos
     * @param complete          {@code true} solo si envío e impuestos son conocidos
     * @param comparisonLabel   etiqueta transparente para la interfaz («Total estimado» o «Menor precio publicado»)
     */
    public record PriceBreakdown(Money knownTotal, boolean complete, String comparisonLabel) {
    }

    /**
     * Calcula el precio total conocido en la moneda original indicando si está completo.
     *
     * @param basePrice     precio publicado
     * @param shipping      costo de envío (nulo si es desconocido)
     * @param shippingKnown indica si el envío es conocido
     * @param tax           impuesto conocido (nulo si es desconocido)
     * @param taxKnown      indica si el impuesto es conocido
     * @return desglose transparente de precio
     */
    public PriceBreakdown calculateFinalPrice(
            Money basePrice,
            Money shipping,
            boolean shippingKnown,
            Money tax,
            boolean taxKnown
    ) {
        Money total = basePrice;
        if (shippingKnown && shipping != null) {
            total = total.plus(shipping);
        }
        if (taxKnown && tax != null) {
            total = total.plus(tax);
        }
        boolean complete = shippingKnown && taxKnown;
        String label = complete ? "Total estimado" : "Total estimado incompleto (costos pendientes)";
        return new PriceBreakdown(total, complete, label);
    }

    /**
     * Convierte un monto a la moneda destino usando la tasa dada o la tasa de referencia USD/COP.
     *
     * @param money          monto origen
     * @param targetCurrency código de moneda destino
     * @param customRate     tasa de cambio opcional (si es nula usa referencia USD/COP)
     * @return monto convertido
     */
    public Money convert(Money money, String targetCurrency, BigDecimal customRate) {
        if (money.getCurrency().equalsIgnoreCase(targetCurrency)) {
            return money;
        }
        if (customRate != null && customRate.compareTo(BigDecimal.ZERO) > 0) {
            return money.convertTo(targetCurrency, customRate);
        }
        if ("USD".equalsIgnoreCase(money.getCurrency()) && "COP".equalsIgnoreCase(targetCurrency)) {
            return money.convertTo("COP", DEFAULT_USD_TO_COP);
        }
        if ("COP".equalsIgnoreCase(money.getCurrency()) && "USD".equalsIgnoreCase(targetCurrency)) {
            BigDecimal inverse = BigDecimal.ONE.divide(DEFAULT_USD_TO_COP, 6, RoundingMode.HALF_UP);
            return money.convertTo("USD", inverse);
        }
        throw new IllegalArgumentException("No existe tasa de conversión configurada entre "
                + money.getCurrency() + " y " + targetCurrency);
    }
}
