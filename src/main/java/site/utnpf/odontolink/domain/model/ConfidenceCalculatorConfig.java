package site.utnpf.odontolink.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Configuracion inmutable del calculador de confianza (RF34).
 *
 * <p>Vive en el dominio para que el calculador no dependa de Spring ni de
 * properties externas: cualquier orquestador (test, batch, otro modulo)
 * puede construir un config y obtener resultados deterministicos. La
 * infraestructura ({@code ConfidenceCalculatorProperties}) adapta sus
 * properties a un {@link ConfidenceCalculatorConfig} al levantar el bean.
 *
 * <p>Convenciones:
 * <ul>
 *   <li>Los pesos NO se validan que sumen 1.0; el calculador asume que el
 *       operador los configuro coherentemente. Validar la suma seria
 *       arbitrario y se interpondria con futuras escalas (ej. tres senales
 *       sumando 1.2 con normalizacion implicita).</li>
 *   <li>Los patrones de refusal se aceptan en su forma cruda. El calculador
 *       (y el {@code RefusalDetector}) los normalizan via
 *       {@link #normalize(String)} antes de matchear; los tests usan
 *       exactamente la misma normalizacion.</li>
 *   <li>Los textos de categoria se devuelven tal cual; centralizar la copia
 *       aqui evita que el dominio dependa de bundles externos.</li>
 * </ul>
 */
public record ConfidenceCalculatorConfig(
        double normalizationFactor,
        double weightRetrieval,
        double weightCoverage,
        double strongRetrievalThreshold,
        double strongCoverageThreshold,
        double chunkRelevanceThreshold,
        int expectedRelevantChunks,
        List<Double> topKWeights,
        double alphaNoRetrieval,
        double alphaShortReply,
        int shortReplyThresholdChars,
        List<String> refusalPatterns,
        CategoryMessages messages
) {

    public ConfidenceCalculatorConfig {
        Objects.requireNonNull(messages, "messages");
        if (normalizationFactor <= 0) {
            throw new IllegalArgumentException("normalizationFactor debe ser > 0, fue " + normalizationFactor);
        }
        if (expectedRelevantChunks <= 0) {
            throw new IllegalArgumentException("expectedRelevantChunks debe ser > 0, fue " + expectedRelevantChunks);
        }
        if (shortReplyThresholdChars < 0) {
            throw new IllegalArgumentException(
                    "shortReplyThresholdChars debe ser >= 0, fue " + shortReplyThresholdChars);
        }
        topKWeights = (topKWeights == null || topKWeights.isEmpty())
                ? List.of(0.5, 0.3, 0.2) : List.copyOf(topKWeights);
        refusalPatterns = refusalPatterns == null ? List.of() : List.copyOf(refusalPatterns);
    }

    /**
     * Devuelve los patrones de refusal ya normalizados (lowercase + sin
     * tildes), listos para matching por substring. Centralizar la
     * normalizacion aqui asegura que el detector y los tests usen el mismo
     * algoritmo.
     */
    public List<String> normalizedRefusalPatterns() {
        return refusalPatterns.stream().map(ConfidenceCalculatorConfig::normalize).toList();
    }

    /**
     * Normaliza texto: lowercase + remueve tildes / dieresis / enie.
     * Suficiente para el espaniol del set de patrones del PoC; si se agregan
     * idiomas, extender la tabla aqui.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case 'á' -> out.append('a');
                case 'é' -> out.append('e');
                case 'í' -> out.append('i');
                case 'ó' -> out.append('o');
                case 'ú', 'ü' -> out.append('u');
                case 'ñ' -> out.append('n');
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Textos por categoria. Inmutable, sin defaults: el caller los provee
     * siempre (la fuente unica de copy vive en application.properties via
     * {@code ConfidenceCalculatorProperties.Messages}).
     */
    public record CategoryMessages(
            String officialLabel,
            String officialBody,
            String partialLabel,
            String partialBody,
            String generalLabel,
            String generalBody,
            String outOfScopeLabel,
            String outOfScopeBody
    ) {
        public CategoryMessages {
            Objects.requireNonNull(officialLabel, "officialLabel");
            Objects.requireNonNull(officialBody, "officialBody");
            Objects.requireNonNull(partialLabel, "partialLabel");
            Objects.requireNonNull(partialBody, "partialBody");
            Objects.requireNonNull(generalLabel, "generalLabel");
            Objects.requireNonNull(generalBody, "generalBody");
            Objects.requireNonNull(outOfScopeLabel, "outOfScopeLabel");
            Objects.requireNonNull(outOfScopeBody, "outOfScopeBody");
        }
    }
}
