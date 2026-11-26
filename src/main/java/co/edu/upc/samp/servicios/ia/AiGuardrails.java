package co.edu.upc.samp.servicios.ia;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Capa de protección de entrada ({@code Guardrails}) para el asistente de IA.
 * Bloquea intentos de inyección de instrucciones y redacta datos personales sensibles (PII).
 */
@Component
public class AiGuardrails {

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "ignore previous instructions",
            "ignora las instrucciones anteriores",
            "drop table",
            "delete from",
            "system prompt"
    );

    /**
     * Resultado de la evaluación de seguridad de una entrada del usuario.
     *
     * @param allowed   {@code true} si la entrada puede procesarse
     * @param sanitized texto limpio sin datos personales expuestos
     * @param reason    motivo del bloqueo si {@code allowed} es {@code false}
     */
    public record GuardResult(boolean allowed, String sanitized, String reason) {
    }

    /**
     * Evalúa y limpia la consulta del usuario antes de enviarla al orquestador de IA.
     *
     * @param rawInput texto original escrito por el usuario
     * @return resultado de validación de seguridad
     */
    public GuardResult screen(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return new GuardResult(false, "", "Escribe una pregunta o describe qué deseas armar.");
        }
        String lower = rawInput.toLowerCase(Locale.ROOT);
        for (String pattern : BLOCKED_PATTERNS) {
            if (lower.contains(pattern)) {
                return new GuardResult(false, "",
                        "Tu mensaje contiene instrucciones no permitidas. El asistente solo consulta productos y arma listas.");
            }
        }
        String redacted = rawInput
                .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[correo-oculto]")
                .replaceAll("\\b\\d{10,16}\\b", "[numero-oculto]");
        return new GuardResult(true, redacted.trim(), null);
    }
}
