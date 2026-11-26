package co.edu.upc.samp.servicios;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de análisis histórico de 30 días y recomendación «Comprar o esperar» (RF-22).
 *
 * <p>Compara el precio actual frente al mínimo, máximo y promedio móvil de los últimos
 * 30 días para emitir un consejo transparente basado en datos observables.
 */
@Service
public class BuyOrWaitAdvisor {

    /**
     * Punto diario del historial de precios de un producto.
     *
     * @param date     fecha de observación
     * @param priceCop precio total estimado en COP
     */
    public record PricePoint(LocalDate date, BigDecimal priceCop) {
    }

    /**
     * Resultado del análisis «Comprar o esperar».
     *
     * @param recommendation etiqueta clara («Conviene comprar», «Precio estable» o «Conviene esperar»)
     * @param reason         explicación transparente basada en estadísticas de 30 días
     * @param currentCop     precio actual en COP
     * @param min30dCop      mínimo observado en 30 días
     * @param max30dCop      máximo observado en 30 días
     * @param avg30dCop      promedio de los últimos 30 días
     * @param deltaVsAvgPct  variación porcentual respecto al promedio
     * @param history        serie histórica de 30 puntos diarios
     */
    public record BuyAdvice(
            String recommendation,
            String reason,
            BigDecimal currentCop,
            BigDecimal min30dCop,
            BigDecimal max30dCop,
            BigDecimal avg30dCop,
            double deltaVsAvgPct,
            List<PricePoint> history
    ) {
    }

    /**
     * Genera una serie histórica determinística de 30 días marcada como {@code DEMO} cuando
     * la base de datos aún no acumula 30 días reales de recolección.
     *
     * @param seedKey    clave estable del producto (GTIN o título)
     * @param currentCop precio actual de referencia en COP
     * @return lista ordenada de 30 puntos diarios
     */
    public List<PricePoint> buildDemoHistory30Days(String seedKey, BigDecimal currentCop) {
        List<PricePoint> points = new ArrayList<>(30);
        int hash = Math.abs(seedKey == null ? 42 : seedKey.hashCode());
        LocalDate today = LocalDate.now();
        for (int daysAgo = 29; daysAgo >= 1; daysAgo--) {
            double wave = Math.sin((daysAgo + (hash % 11)) * 0.45) * 0.06
                    + (((hash + daysAgo * 17) % 9) - 3) * 0.008;
            BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(wave));
            BigDecimal pointPrice = currentCop.multiply(factor).setScale(0, RoundingMode.HALF_UP);
            points.add(new PricePoint(today.minusDays(daysAgo), pointPrice));
        }
        points.add(new PricePoint(today, currentCop.setScale(0, RoundingMode.HALF_UP)));
        return points;
    }

    /**
     * Evalúa una serie histórica de al menos 30 días y determina si conviene comprar o esperar.
     *
     * @param currentCop precio actual en COP
     * @param history    serie histórica ordenada cronológicamente
     * @return recomendación fundamentada
     */
    public BuyAdvice advise(BigDecimal currentCop, List<PricePoint> history) {
        List<PricePoint> series = (history == null || history.isEmpty())
                ? List.of(new PricePoint(LocalDate.now(), currentCop))
                : history;

        BigDecimal min = series.stream().map(PricePoint::priceCop).min(BigDecimal::compareTo).orElse(currentCop);
        BigDecimal max = series.stream().map(PricePoint::priceCop).max(BigDecimal::compareTo).orElse(currentCop);
        BigDecimal sum = series.stream().map(PricePoint::priceCop).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(series.size()), 0, RoundingMode.HALF_UP);

        double deltaPct = avg.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : currentCop.subtract(avg)
                .multiply(BigDecimal.valueOf(100))
                .divide(avg, 1, RoundingMode.HALF_UP)
                .doubleValue();

        String recommendation;
        String reason;
        if (deltaPct <= -3.0 || currentCop.compareTo(min) <= 0) {
            recommendation = "Conviene comprar";
            reason = String.format("El precio actual está %.1f%% por debajo del promedio de los últimos %d días.",
                    Math.abs(deltaPct), series.size());
        } else if (deltaPct >= 4.0) {
            recommendation = "Conviene esperar";
            reason = String.format("El precio actual está %.1f%% por encima del promedio de %d días; conviene crear una alerta.",
                    deltaPct, series.size());
        } else {
            recommendation = "Precio estable";
            reason = String.format("El precio actual se mantiene cerca del promedio de los últimos %d días (%+.1f%%).",
                    series.size(), deltaPct);
        }

        return new BuyAdvice(recommendation, reason, currentCop, min, max, avg, deltaPct, series);
    }
}
