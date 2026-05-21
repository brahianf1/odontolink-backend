package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig;

import java.util.List;
import java.util.Objects;

/**
 * Detector de respuestas evasivas o de rechazo del chatbot (RF34).
 *
 * <p>Servicio de dominio puro: depende unicamente de
 * {@link ConfidenceCalculatorConfig} (otro tipo del dominio) y no toca
 * infraestructura. Cualquier orquestador puede instanciarlo y obtener
 * resultados deterministicos.
 *
 * <p>El matching se hace en dos pasos:
 * <ol>
 *   <li>Pre-normalizamos los patrones una vez en construccion (lowercase
 *       + sin tildes) — los teniamos que normalizar igual y hacerlo en
 *       cada turno seria desperdicio.</li>
 *   <li>Normalizamos el reply con la misma transformacion y buscamos
 *       cualquier patron por substring.</li>
 * </ol>
 * No usamos regex: las frases del PoC son substrings comunes y un
 * {@code String#contains} es mas barato y mas predecible.
 */
public class RefusalDetector {

    private final List<String> normalizedPatterns;

    public RefusalDetector(ConfidenceCalculatorConfig config) {
        Objects.requireNonNull(config, "config");
        this.normalizedPatterns = config.normalizedRefusalPatterns();
    }

    /**
     * {@code true} si el reply contiene al menos uno de los patrones de
     * rechazo configurados. {@code false} para reply nulo, vacio o cuando
     * la lista de patrones esta vacia.
     */
    public boolean isRefusal(String reply) {
        if (reply == null || reply.isBlank() || normalizedPatterns.isEmpty()) {
            return false;
        }
        String normalized = ConfidenceCalculatorConfig.normalize(reply);
        for (String pattern : normalizedPatterns) {
            if (pattern != null && !pattern.isBlank() && normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
