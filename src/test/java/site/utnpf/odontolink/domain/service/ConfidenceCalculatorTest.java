package site.utnpf.odontolink.domain.service;

import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.RetrievalDocument;
import site.utnpf.odontolink.domain.model.ConfidenceAssessment;
import site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig;
import site.utnpf.odontolink.domain.model.ConfidenceCategory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests de {@link ConfidenceCalculator}.
 *
 * <p>Cubre las dos responsabilidades: clasificacion en una de las cuatro
 * categorias y calculo numerico del score con normalizacion + penalizaciones.
 * Los datos de ejemplo siguen la escala observada en el PoC (raw scores
 * [0.69, 12.87]).
 */
class ConfidenceCalculatorTest {

    private static final List<String> NO_REFUSAL_PATTERNS = List.of();
    private static final List<String> WITH_REFUSAL_PATTERN = List.of(
            "no puedo procesar esta solicitud");

    private static ConfidenceCalculatorConfig defaultConfig(List<String> patterns) {
        return new ConfidenceCalculatorConfig(
                10.0,                   // normalizationFactor
                0.7, 0.3,               // weights
                0.4, 0.5,               // strong thresholds
                0.4, 3,                 // chunk relevance threshold, expected
                List.of(0.5, 0.3, 0.2), // top-k weights
                0.5, 0.7, 30,           // alpha no-rag, short-reply, threshold
                patterns,
                new ConfidenceCalculatorConfig.CategoryMessages(
                        "OFICIAL", "off body",
                        "PARCIAL", "par body",
                        "GENERAL", "gen body",
                        "FUERA",   "out body")
        );
    }

    private static RetrievalDocument chunk(double score) {
        return new RetrievalDocument("uuid", score);
    }

    private static ConfidenceCalculator newCalculator(List<String> patterns) {
        ConfidenceCalculatorConfig config = defaultConfig(patterns);
        return new ConfidenceCalculator(config, new RefusalDetector(config));
    }

    // -------- Clasificacion ------------------------------------------

    @Test
    void rechazoDetectado_clasificaComoOutOfScope() {
        ConfidenceCalculator calc = newCalculator(WITH_REFUSAL_PATTERN);
        ConfidenceAssessment a = calc.assess(
                "Lo sentimos, no puedo procesar esta solicitud.",
                List.of(chunk(5.0)));
        assertEquals(ConfidenceCategory.OUT_OF_SCOPE, a.category());
        assertEquals("FUERA", a.label());
        assertEquals("out body", a.message());
    }

    @Test
    void sinChunks_clasificaComoGeneral() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Esta es una respuesta general sin RAG, lo suficientemente larga.",
                List.of());
        assertEquals(ConfidenceCategory.GENERAL, a.category());
    }

    @Test
    void chunksFuertesPorRetrieval_clasificaComoOfficial() {
        // S_A pasa el umbral (0.4). S_C puede estar bajo: alcanza con OR.
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta institucional larga sobre tratamientos de la facultad.",
                List.of(chunk(8.0), chunk(2.0), chunk(2.0)));
        assertEquals(ConfidenceCategory.OFFICIAL, a.category());
    }

    @Test
    void chunksFuertesPorCobertura_clasificaComoOfficial() {
        // S_A bajo (~0.4), pero S_C alto (3/3 chunks >= 0.4 normalized = >=4 raw).
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta institucional larga sobre tratamientos de la facultad.",
                List.of(chunk(4.0), chunk(4.0), chunk(4.0)));
        assertEquals(ConfidenceCategory.OFFICIAL, a.category());
    }

    @Test
    void chunksDebiles_clasificaComoPartial() {
        // S_A < 0.4 Y S_C < 0.5: cae a parcial.
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta razonable larga sobre el tema consultado.",
                List.of(chunk(2.0), chunk(1.0), chunk(1.0)));
        assertEquals(ConfidenceCategory.PARTIAL, a.category());
    }

    // -------- Score numerico -----------------------------------------

    @Test
    void scorePerfectoSinPenalizacion_devuelve100() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta detallada institucional con cobertura amplia y excelente score.",
                List.of(chunk(20.0), chunk(20.0), chunk(20.0)));
        assertEquals(100, a.score());
        assertEquals(1.0, a.alpha(), 1e-9);
        assertEquals(1.0, a.signalRetrieval(), 1e-9);
        assertEquals(1.0, a.signalCoverage(), 1e-9);
    }

    @Test
    void scoreSinChunks_aplicaPenalizacionNoRetrieval() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta general lo suficientemente larga para evitar la penalizacion short.",
                List.of());
        // S_A=0, S_C=0, alpha=0.5 -> score = 0.
        assertEquals(0, a.score());
        assertEquals(0.5, a.alpha(), 1e-9);
    }

    @Test
    void scoreReplyMuyCorto_aplicaPenalizacionShort() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        // S_A=1.0, S_C=1.0, alpha=0.7 (reply < 30 chars). composite=1, score=70.
        ConfidenceAssessment a = calc.assess("Si.",
                List.of(chunk(20.0), chunk(20.0), chunk(20.0)));
        assertEquals(70, a.score());
        assertEquals(0.7, a.alpha(), 1e-9);
    }

    @Test
    void scoreSinChunksYReplyCorto_multiplicaPenalizaciones() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess("No.", List.of());
        // alpha = 0.5 * 0.7 = 0.35. Senales 0. Score 0.
        assertEquals(0.35, a.alpha(), 1e-9);
        assertEquals(0, a.score());
    }

    @Test
    void filtraScoresNegativosSentinel() {
        // El primer chunk de DO a veces es score=-9.5e9 (sentinel).
        // Debe filtrarse: el top-1 deberia ser el segundo (real).
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta larga sobre el tema consultado por el usuario.",
                List.of(chunk(-9.5e9), chunk(9.0), chunk(6.0), chunk(3.0)));
        // S_A esperado: 0.5*0.9 + 0.3*0.6 + 0.2*0.3 = 0.69
        assertEquals(0.69, a.signalRetrieval(), 1e-9);
    }

    @Test
    void scoreSiempreEnRango0a100() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        for (double s : new double[]{0.0, 0.5, 5.0, 10.0, 50.0, 1000.0}) {
            for (int n : new int[]{0, 1, 3, 5}) {
                List<RetrievalDocument> chunks = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) chunks.add(chunk(s));
                ConfidenceAssessment a = calc.assess("Respuesta de tamano normal mas de treinta chars.", chunks);
                assertTrue(a.score() >= 0 && a.score() <= 100,
                        "score fuera de rango para s=" + s + " n=" + n + ": " + a.score());
            }
        }
    }

    // -------- Casos borde --------------------------------------------

    @Test
    void chunksNullNoExplota() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess("Respuesta cualquiera de longitud razonable.", null);
        assertEquals(ConfidenceCategory.GENERAL, a.category());
    }

    @Test
    void replyNullNoExplota() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(null, List.of(chunk(5.0)));
        assertNotNull(a);
        // Reply null + chunks: no es refusal (sin texto que matchear), hay chunks,
        // senales calculadas; reply length=0 => penalizacion short aplicada.
        assertEquals(0.7, a.alpha(), 1e-9);
    }

    @Test
    void topKConPocosChunksRenormalizaPesos() {
        // Con 1 solo chunk, S_A debe ser igual al score normalizado (peso = 1.0).
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta razonable con un solo chunk fuerte de longitud normal.",
                List.of(chunk(8.0)));
        assertEquals(0.8, a.signalRetrieval(), 1e-9);
    }

    @Test
    void breakdownExpuesto() {
        ConfidenceCalculator calc = newCalculator(NO_REFUSAL_PATTERNS);
        ConfidenceAssessment a = calc.assess(
                "Respuesta de longitud normal sobre el tema con buena cobertura.",
                List.of(chunk(5.0), chunk(4.0)));
        // Verifica que las senales individuales son auditables.
        assertTrue(a.signalRetrieval() > 0);
        assertTrue(a.signalCoverage() >= 0);
        assertTrue(a.alpha() > 0);
        assertNotNull(a.label());
        assertNotNull(a.message());
    }
}
