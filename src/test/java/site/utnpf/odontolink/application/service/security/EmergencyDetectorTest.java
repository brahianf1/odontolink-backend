package site.utnpf.odontolink.application.service.security;

import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.model.EmergencyKeyword;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del detector local de emergencias (RF32).
 */
class EmergencyDetectorTest {

    private final EmergencyDetector detector = new EmergencyDetector();

    private EmergencyKeyword kw(String term) {
        return new EmergencyKeyword(1L, term, true, Instant.now(), Instant.now());
    }

    @Test
    void detectaTerminoExacto() {
        boolean hit = detector.containsEmergencyTerm(
                "Tengo sangrado en la encia", List.of(kw("sangrado")));
        assertTrue(hit);
    }

    @Test
    void detectaTerminoEnMayusculas() {
        boolean hit = detector.containsEmergencyTerm(
                "TENGO SANGRADO EN LA ENCIA", List.of(kw("sangrado")));
        assertTrue(hit);
    }

    @Test
    void detectaTerminoConAcentos() {
        // El input tiene "infeccion" sin acento; la keyword "infección" con
        // acento debe matchear gracias a la normalizacion NFD.
        boolean hit = detector.containsEmergencyTerm(
                "tengo una infeccion severa", List.of(kw("infección")));
        assertTrue(hit);
    }

    @Test
    void noMatcheaCuandoKeywordNoEsta() {
        boolean hit = detector.containsEmergencyTerm(
                "Hola, queria sacar turno", List.of(kw("sangrado")));
        assertFalse(hit);
    }

    @Test
    void noMatcheaKeywordInactiva() {
        EmergencyKeyword inactive = new EmergencyKeyword(1L, "sangrado", false, Instant.now(), Instant.now());
        boolean hit = detector.containsEmergencyTerm(
                "Tengo sangrado en la encia", List.of(inactive));
        assertFalse(hit);
    }

    @Test
    void inputVacioNoMatchea() {
        boolean hit = detector.containsEmergencyTerm("", List.of(kw("sangrado")));
        assertFalse(hit);
    }

    @Test
    void diccionarioVacioNoMatchea() {
        boolean hit = detector.containsEmergencyTerm("Tengo sangrado", List.of());
        assertFalse(hit);
    }
}
