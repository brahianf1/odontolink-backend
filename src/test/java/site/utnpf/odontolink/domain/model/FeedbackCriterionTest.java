package site.utnpf.odontolink.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackCriterionTest {

    @Test
    @DisplayName("criterio nuevo y activo: deactivatedAt es null")
    void newCriterion_deactivatedAtIsNull() {
        FeedbackCriterion c = sample(true);
        assertNull(c.getDeactivatedAt());
    }

    @Test
    @DisplayName("setActive(false) sobre activo: setea deactivatedAt")
    void activeToInactive_setsTimestamp() {
        FeedbackCriterion c = sample(true);
        c.setActive(false);
        assertNotNull(c.getDeactivatedAt());
    }

    @Test
    @DisplayName("setActive(true) sobre inactivo: nulea deactivatedAt")
    void inactiveToActive_clearsTimestamp() {
        FeedbackCriterion c = sample(true);
        c.setActive(false);
        assertNotNull(c.getDeactivatedAt());
        c.setActive(true);
        assertNull(c.getDeactivatedAt());
    }

    @Test
    @DisplayName("setActive(true) sobre activo: no toca deactivatedAt")
    void activeToActive_noop() {
        FeedbackCriterion c = sample(true);
        c.setActive(true);
        assertNull(c.getDeactivatedAt());
    }

    @Test
    @DisplayName("setActive(false) sobre inactivo: deactivatedAt se mantiene (no se reescribe)")
    void inactiveToInactive_preservesTimestamp() {
        FeedbackCriterion c = sample(true);
        c.setActive(false);
        var first = c.getDeactivatedAt();
        // Forzamos un re-set falso a falso; debe ser idempotente.
        c.setActive(false);
        assertTrue(c.getDeactivatedAt() == first || c.getDeactivatedAt().equals(first));
    }

    private static FeedbackCriterion sample(boolean active) {
        return new FeedbackCriterion(
                "PUNCTUALITY", "Puntualidad", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER,
                true, 1, active);
    }
}
