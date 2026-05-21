package site.utnpf.odontolink.domain.service;

import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link RefusalDetector}.
 *
 * <p>Foco: precision del matching contra los patrones del PoC y robustez
 * frente a variantes ortograficas (tildes, mayusculas, signos).
 */
class RefusalDetectorTest {

    private static ConfidenceCalculatorConfig configWith(List<String> patterns) {
        return new ConfidenceCalculatorConfig(
                10.0, 0.7, 0.3, 0.4, 0.5, 0.4, 3,
                List.of(0.5, 0.3, 0.2),
                0.5, 0.7, 30,
                patterns,
                fakeMessages()
        );
    }

    private static ConfidenceCalculatorConfig.CategoryMessages fakeMessages() {
        return new ConfidenceCalculatorConfig.CategoryMessages(
                "ofL", "ofB", "paL", "paB", "geL", "geB", "ooL", "ooB"
        );
    }

    @Test
    void detectaGuardrailCanonicoDeDO() {
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("no puedo procesar esta solicitud")));
        assertTrue(detector.isRefusal(
                "Lo sentimos, pero no puedo procesar esta solicitud. Por razones de seguridad..."));
    }

    @Test
    void detectaRechazoConTildes() {
        // Patron sin tildes; reply con tildes. Debe matchear igual.
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("mi rol esta estrictamente")));
        assertTrue(detector.isRefusal(
                "Mi rol está estrictamente enfocado en la plataforma OdontoLink."));
    }

    @Test
    void detectaRechazoConMayusculas() {
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("no puedo proporcionar informacion")));
        assertTrue(detector.isRefusal(
                "DISCULPÁ, NO PUEDO PROPORCIONAR INFORMACIÓN sobre el clima."));
    }

    @Test
    void noFalsoPositivoEnRespuestaInformativa() {
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("no puedo procesar esta solicitud", "fuera del scope")));
        // Respuesta totalmente legitima sobre tratamientos.
        assertFalse(detector.isRefusal(
                "En la clinica se realizan tratamientos de operatoria, ortodoncia y endodoncia."));
    }

    @Test
    void replyVacioNoEsRefusal() {
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("no puedo procesar esta solicitud")));
        assertFalse(detector.isRefusal(""));
    }

    @Test
    void replyNullNoEsRefusal() {
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("no puedo procesar esta solicitud")));
        assertFalse(detector.isRefusal(null));
    }

    @Test
    void listaVaciaDePatronesNoMatcheaNada() {
        // Defensa: si el operador limpia la lista de patrones, el detector
        // simplemente no marca nada como refusal (no rompe el flujo).
        RefusalDetector detector = new RefusalDetector(configWith(List.of()));
        assertFalse(detector.isRefusal(
                "No puedo procesar esta solicitud, lo siento."));
    }

    @Test
    void matcheaUnoDeVariosPatrones() {
        RefusalDetector detector = new RefusalDetector(configWith(List.of(
                "no puedo procesar esta solicitud",
                "estrictamente enfocado en la plataforma",
                "fuera del scope"
        )));
        assertTrue(detector.isRefusal(
                "Mi rol esta estrictamente enfocado en la plataforma OdontoLink."));
    }

    @Test
    void patronConTildesNormalizadoIgualQueReply() {
        // Patron con tildes en config; deberia normalizarse y matchear reply
        // sin tildes. Garantiza simetria.
        RefusalDetector detector = new RefusalDetector(configWith(
                List.of("razónes de seguridad")));
        assertTrue(detector.isRefusal(
                "Lo siento, por razones de seguridad no puedo responder."));
    }
}
