package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.RetrievalDocument;
import site.utnpf.odontolink.domain.model.ConfidenceAssessment;
import site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig;
import site.utnpf.odontolink.domain.model.ConfidenceCategory;

import java.util.List;
import java.util.Objects;

/**
 * Calculador de confianza categorica del chatbot (RF34).
 *
 * <p>Implementa la Confianza Compuesta Ponderada (CCP) reducida a dos
 * senales validadas en el PoC de mayo 2026:
 *
 * <pre>
 *   S_A (retrieval) = weighted_avg(top-K normalized chunk scores)
 *   S_C (coverage)  = min(1, |chunks relevantes| / expected)
 *
 *   composite = w_A * S_A + w_C * S_C
 *   score     = round(100 * composite * alpha)        ∈ [0, 100]
 * </pre>
 *
 * <p>Clasifica el resultado en una de cuatro {@link ConfidenceCategory
 * categorias} con prioridad: rechazo &gt; sin RAG &gt; chunks fuertes &gt;
 * chunks debiles. El mapeo a label/message se hace via
 * {@link ConfidenceCalculatorConfig.CategoryMessages} para que el copy sea
 * editable sin tocar codigo.
 *
 * <p>Hallazgo del PoC encarnado aqui: los raw scores de DO Gradient
 * <strong>no</strong> vienen en [0, 1] (rango observado: [0.69, 12.87], con
 * outliers negativos centinela ~-9.5e9). Por eso normalizamos: filtramos
 * scores negativos y dividimos por {@code normalizationFactor} con clamp.
 *
 * <p>El calculador es <strong>puro</strong>: no toca BD, no llama a APIs,
 * es deterministico. Toda configuracion entra por
 * {@link ConfidenceCalculatorConfig} y el {@link RefusalDetector} ya
 * configurado.
 */
public class ConfidenceCalculator {

    private final ConfidenceCalculatorConfig config;
    private final RefusalDetector refusalDetector;

    public ConfidenceCalculator(ConfidenceCalculatorConfig config,
                                RefusalDetector refusalDetector) {
        this.config = Objects.requireNonNull(config, "config");
        this.refusalDetector = Objects.requireNonNull(refusalDetector, "refusalDetector");
    }

    /**
     * Computa el assessment dada la respuesta del bot y los chunks RAG
     * devueltos por el proveedor. Nunca devuelve {@code null}: la decision
     * de ocultar el indicador (emergencia, fallback, toggle off, PII) la
     * toma el caller, no este metodo.
     */
    public ConfidenceAssessment assess(String reply, List<RetrievalDocument> chunks) {
        List<RetrievalDocument> safeChunks = chunks == null ? List.of() : chunks;
        boolean refusal = refusalDetector.isRefusal(reply);
        boolean hasChunks = !safeChunks.isEmpty();

        double sA = signalRetrieval(safeChunks);
        double sC = signalCoverage(safeChunks);
        double alpha = computeAlpha(reply, hasChunks);

        double composite = config.weightRetrieval() * sA + config.weightCoverage() * sC;
        int score = clampPercent((int) Math.round(100.0 * composite * alpha));

        ConfidenceCategory category = classify(refusal, hasChunks, sA, sC);
        return new ConfidenceAssessment(
                category,
                labelFor(category),
                messageFor(category),
                score,
                sA,
                sC,
                alpha
        );
    }

    // --- Senales ---------------------------------------------------------

    /**
     * S_A: promedio ponderado del top-K (K = tamano de
     * {@code config.topKWeights()}) sobre scores normalizados, ordenados
     * descendentemente. Si hay menos chunks que K, los pesos se renormalizan
     * sobre los presentes.
     */
    private double signalRetrieval(List<RetrievalDocument> chunks) {
        if (chunks.isEmpty()) {
            return 0.0;
        }
        List<Double> weights = config.topKWeights();
        int k = weights.size();

        // Normalizamos PRIMERO y despues ordenamos; los sentinels filtrados a 0
        // no se cuelan al top como artefacto del orden.
        double[] normalized = chunks.stream()
                .mapToDouble(c -> normalizeScore(c.score()))
                .sorted()
                .toArray();

        // sorted() es ascendente; tomamos los ultimos k.
        int take = Math.min(k, normalized.length);
        double[] top = new double[take];
        for (int i = 0; i < take; i++) {
            top[i] = normalized[normalized.length - 1 - i];
        }

        double totalWeight = 0;
        double weighted = 0;
        for (int i = 0; i < take; i++) {
            double w = weights.get(i);
            totalWeight += w;
            weighted += w * top[i];
        }
        if (totalWeight == 0) {
            return 0.0;
        }
        return clampUnit(weighted / totalWeight);
    }

    /**
     * S_C: fraccion de chunks "relevantes" (score normalizado &gt;= threshold)
     * sobre los esperados, saturando arriba a 1.
     */
    private double signalCoverage(List<RetrievalDocument> chunks) {
        int expected = config.expectedRelevantChunks();
        if (chunks.isEmpty() || expected <= 0) {
            return 0.0;
        }
        long relevant = chunks.stream()
                .mapToDouble(c -> normalizeScore(c.score()))
                .filter(s -> s >= config.chunkRelevanceThreshold())
                .count();
        return clampUnit((double) relevant / expected);
    }

    private double normalizeScore(double raw) {
        if (raw < 0.0) {
            // Sentinels (~-9.5e9) y cualquier negativo defensivamente => 0.
            return 0.0;
        }
        double factor = config.normalizationFactor();
        // factor > 0 garantizado por la validacion del record; igual defendemos.
        if (factor <= 0.0) {
            return clampUnit(raw);
        }
        return clampUnit(raw / factor);
    }

    private double computeAlpha(String reply, boolean hasChunks) {
        double alpha = 1.0;
        if (!hasChunks) {
            alpha *= config.alphaNoRetrieval();
        }
        int threshold = config.shortReplyThresholdChars();
        int effectiveLength = reply == null ? 0 : reply.strip().length();
        if (effectiveLength < threshold) {
            alpha *= config.alphaShortReply();
        }
        return clampUnit(alpha);
    }

    // --- Clasificacion --------------------------------------------------

    /**
     * Reglas de prioridad (la primera que matchea gana):
     * <ol>
     *   <li>Rechazo detectado -&gt; OUT_OF_SCOPE</li>
     *   <li>Sin chunks (RAG no activo) -&gt; GENERAL</li>
     *   <li>S_A &gt;= strongRetrieval OR S_C &gt;= strongCoverage -&gt; OFFICIAL</li>
     *   <li>Default -&gt; PARTIAL</li>
     * </ol>
     */
    private ConfidenceCategory classify(boolean refusal,
                                        boolean hasChunks,
                                        double sA,
                                        double sC) {
        if (refusal) {
            return ConfidenceCategory.OUT_OF_SCOPE;
        }
        if (!hasChunks) {
            return ConfidenceCategory.GENERAL;
        }
        if (sA >= config.strongRetrievalThreshold() || sC >= config.strongCoverageThreshold()) {
            return ConfidenceCategory.OFFICIAL;
        }
        return ConfidenceCategory.PARTIAL;
    }

    // --- Mensajes -------------------------------------------------------

    private String labelFor(ConfidenceCategory category) {
        ConfidenceCalculatorConfig.CategoryMessages m = config.messages();
        return switch (category) {
            case OFFICIAL     -> m.officialLabel();
            case PARTIAL      -> m.partialLabel();
            case GENERAL      -> m.generalLabel();
            case OUT_OF_SCOPE -> m.outOfScopeLabel();
        };
    }

    private String messageFor(ConfidenceCategory category) {
        ConfidenceCalculatorConfig.CategoryMessages m = config.messages();
        return switch (category) {
            case OFFICIAL     -> m.officialBody();
            case PARTIAL      -> m.partialBody();
            case GENERAL      -> m.generalBody();
            case OUT_OF_SCOPE -> m.outOfScopeBody();
        };
    }

    // --- Utilidades -----------------------------------------------------

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double clampUnit(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
