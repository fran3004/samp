package co.edu.upc.samp;

import co.edu.upc.samp.entidades.CircuitState;
import co.edu.upc.samp.entidades.ConnectorKind;
import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.Money;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.StoreConnectorConfig;
import co.edu.upc.samp.servicios.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de encapsulamiento de dominio ({@link Money}, {@link StoreConnectorConfig})
 * y cálculo transparente de precios ({@link PriceCalculator}).
 */
class DomainAndPriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void moneyShouldPreventNegativeAmountsAndMixedCurrencyAddition() {
        Money cop1 = Money.of(100000, "COP");
        Money cop2 = Money.of(25000, "COP");
        assertThat(cop1.plus(cop2)).isEqualTo(Money.of(125000, "COP"));

        assertThatThrownBy(() -> Money.of(-1, "COP"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cop1.plus(Money.of(10, "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void priceCalculatorShouldFlagIncompleteWhenShippingOrTaxUnknown() {
        PriceCalculator.PriceBreakdown complete = calculator.calculateFinalPrice(
                Money.of(200000, "COP"), Money.of(15000, "COP"), true, Money.zero("COP"), true);
        assertThat(complete.complete()).isTrue();
        assertThat(complete.knownTotal()).isEqualTo(Money.of(215000, "COP"));
        assertThat(complete.comparisonLabel()).isEqualTo("Total estimado");

        PriceCalculator.PriceBreakdown incomplete = calculator.calculateFinalPrice(
                Money.of(200000, "COP"), null, false, Money.zero("COP"), true);
        assertThat(incomplete.complete()).isFalse();
        assertThat(incomplete.knownTotal()).isEqualTo(Money.of(200000, "COP"));
        assertThat(incomplete.comparisonLabel()).contains("incompleto");
    }

    @Test
    void connectorCircuitBreakerShouldOpenAfterThreeConsecutiveFailures() {
        Store store = new Store("TEMU_CO", "CN", "Temu Colombia", "https://www.temu.com/co", "COP", DataKind.DEMO);
        StoreConnectorConfig config = new StoreConnectorConfig(store, "TemuScraperConnector", ConnectorKind.SCRAPER, 1);

        assertThat(config.isAvailable()).isTrue();
        config.recordFailure();
        config.recordFailure();
        assertThat(config.getCircuitState()).isEqualTo(CircuitState.CLOSED);
        config.recordFailure();
        assertThat(config.getCircuitState()).isEqualTo(CircuitState.OPEN);
        assertThat(config.isAvailable()).isFalse();

        config.recordSuccess();
        assertThat(config.getCircuitState()).isEqualTo(CircuitState.CLOSED);
        assertThat(config.getConsecutiveFailures()).isZero();
    }

    @Test
    void priceCalculatorShouldConvertUsdToCop() {
        Money usd = new Money(new BigDecimal("100.00"), "USD");
        Money cop = calculator.convert(usd, "COP", null);
        assertThat(cop).isEqualTo(new Money(new BigDecimal("425000.00"), "COP"));
    }
}
