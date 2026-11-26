package co.edu.upc.samp.servicios.emparejamiento;

import co.edu.upc.samp.entidades.RawOffer;

import java.util.Optional;

/**
 * Estrategia polimórfica de emparejamiento para determinar si dos publicaciones
 * corresponden al mismo producto canónico bajo una regla verificable.
 */
public interface MatchStrategy {

    /**
     * Nivel de prioridad de la estrategia (1 = más estricto, mayor = más flexible).
     *
     * @return nivel numérico
     */
    int level();

    /**
     * Nombre técnico del método de emparejamiento para trazabilidad.
     *
     * @return identificador del método
     */
    String methodName();

    /**
     * Evalúa si dos ofertas crudas representan el mismo producto o son similares.
     *
     * @param reference oferta de referencia
     * @param candidate oferta candidata
     * @return resultado de emparejamiento o vacío si no cumple la regla
     */
    Optional<MatchDecision> evaluate(RawOffer reference, RawOffer candidate);

    /**
     * Resultado inmutable de una evaluación de emparejamiento.
     *
     * @param exactEquivalent {@code true} solo si existe regla verificable; {@code false} si es "similar"
     * @param level           nivel aplicado
     * @param confidence      confianza entre 0.0 y 1.0
     * @param method          nombre del método
     * @param explanation     explicación transparente en español para el usuario
     */
    record MatchDecision(
            boolean exactEquivalent,
            int level,
            double confidence,
            String method,
            String explanation
    ) {
    }
}
