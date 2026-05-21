package site.utnpf.odontolink.domain.model;

import java.util.Objects;

/**
 * Resultado del calculo de confianza para una respuesta del chatbot (RF34).
 *
 * <p>Combina la categoria visible al paciente con el desglose numerico que
 * sirve para observabilidad/admin/tesis. La API publica del chatbot expone
 * la categoria y el mensaje; el score numerico se exhibe solo a admin (o
 * para A/B testing futuro).
 *
 * <p>Convenciones de instanciacion:
 * <ul>
 *   <li>Cuando el agregado decide ocultar el indicador (toggle off, emergencia,
 *       fallback, PII bloqueado), el use case usa {@code null} en vez de
 *       este record. Aqui NUNCA hay un assessment "vacio": si existe, sus
 *       campos son significativos.</li>
 *   <li>Los campos numericos ({@code score}, {@code signalRetrieval},
 *       {@code signalCoverage}, {@code alpha}) son siempre finitos en [0, 1]
 *       (excepto {@code score} que es 0..100 entero). La validacion la hace
 *       el calculador antes de instanciar.</li>
 * </ul>
 *
 * @param category        categoria a mostrar al paciente
 * @param label           titulo corto de la categoria, en castellano
 *                        argentino (ej. "Informacion oficial")
 * @param message         texto explicativo para el paciente, en castellano
 *                        argentino, sin tecnicismos
 * @param score           score CCP entero en [0, 100], para observabilidad y
 *                        admin. NO se expone al paciente
 * @param signalRetrieval valor S_A en [0, 1] (top-k weighted average de
 *                        scores normalizados)
 * @param signalCoverage  valor S_C en [0, 1] (fraccion de chunks relevantes
 *                        sobre los esperados)
 * @param alpha           factor multiplicativo de penalizacion en (0, 1]
 */
public record ConfidenceAssessment(
        ConfidenceCategory category,
        String label,
        String message,
        int score,
        double signalRetrieval,
        double signalCoverage,
        double alpha
) {

    public ConfidenceAssessment {
        Objects.requireNonNull(category, "category es obligatoria");
        Objects.requireNonNull(label, "label es obligatorio");
        Objects.requireNonNull(message, "message es obligatorio");
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score debe estar en [0, 100], fue " + score);
        }
        // Las senales tienen tolerancia por flotantes; clampeamos defensivamente
        // para que ningun consumidor vea valores fuera de [0, 1] por errores
        // de redondeo aguas arriba.
        signalRetrieval = clampUnit(signalRetrieval, "signalRetrieval");
        signalCoverage = clampUnit(signalCoverage, "signalCoverage");
        alpha = clampUnit(alpha, "alpha");
    }

    private static double clampUnit(double value, String field) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(field + " no puede ser NaN");
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
