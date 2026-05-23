package site.utnpf.odontolink.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackCriterionCodes;
import site.utnpf.odontolink.domain.model.FeedbackDirection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedbackCriterionPolicyServiceTest {

    private final FeedbackCriterionPolicyService policy = new FeedbackCriterionPolicyService();

    @Test
    @DisplayName("pasa cuando los scores cubren exactamente el set activo de la dirección")
    void happyPath() {
        List<FeedbackCriterion> active = activeP2P();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(FeedbackCriterionCodes.PUNCTUALITY, 5);
        scores.put(FeedbackCriterionCodes.CARE_QUALITY, 4);
        scores.put(FeedbackCriterionCodes.COMMUNICATION_CLARITY, 5);
        scores.put(FeedbackCriterionCodes.GENERAL_SATISFACTION, 5);

        assertDoesNotThrow(() -> policy.validateScores(
                scores, FeedbackDirection.PATIENT_TO_PRACTITIONER, active));
    }

    @Test
    @DisplayName("rechaza cuando falta un criterio")
    void rejectMissing() {
        List<FeedbackCriterion> active = activeP2P();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(FeedbackCriterionCodes.PUNCTUALITY, 5);
        scores.put(FeedbackCriterionCodes.CARE_QUALITY, 4);
        scores.put(FeedbackCriterionCodes.COMMUNICATION_CLARITY, 5);
        // falta GENERAL_SATISFACTION

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> policy.validateScores(scores, FeedbackDirection.PATIENT_TO_PRACTITIONER, active));
        assert ex.getMessage().contains(FeedbackCriterionCodes.GENERAL_SATISFACTION);
    }

    @Test
    @DisplayName("rechaza criterio que no aplica a la dirección")
    void rejectWrongDirection() {
        List<FeedbackCriterion> active = activeP2P();
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(FeedbackCriterionCodes.PUNCTUALITY, 5);
        scores.put(FeedbackCriterionCodes.CARE_QUALITY, 4);
        scores.put(FeedbackCriterionCodes.COMMUNICATION_CLARITY, 5);
        scores.put(FeedbackCriterionCodes.GENERAL_SATISFACTION, 5);
        scores.put(FeedbackCriterionCodes.PATIENT_BEHAVIOR, 4); // intruso de Pr→Pat

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> policy.validateScores(scores, FeedbackDirection.PATIENT_TO_PRACTITIONER, active));
        assert ex.getMessage().contains(FeedbackCriterionCodes.PATIENT_BEHAVIOR);
    }

    @Test
    @DisplayName("rechaza criterio inactivo aunque mande la dirección correcta")
    void rejectInactiveCriterion() {
        // Misma lista que activeP2P() pero con CARE_QUALITY desactivado.
        FeedbackCriterion punctuality = new FeedbackCriterion(
                FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 1, true);
        FeedbackCriterion careInactive = new FeedbackCriterion(
                FeedbackCriterionCodes.CARE_QUALITY, "Calidad", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 2, false);
        FeedbackCriterion clarity = new FeedbackCriterion(
                FeedbackCriterionCodes.COMMUNICATION_CLARITY, "Claridad", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 3, true);
        FeedbackCriterion satisfaction = new FeedbackCriterion(
                FeedbackCriterionCodes.GENERAL_SATISFACTION, "Satisfacción", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, false, 4, true);

        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(FeedbackCriterionCodes.PUNCTUALITY, 5);
        scores.put(FeedbackCriterionCodes.CARE_QUALITY, 4);
        scores.put(FeedbackCriterionCodes.COMMUNICATION_CLARITY, 5);
        scores.put(FeedbackCriterionCodes.GENERAL_SATISFACTION, 5);

        assertThrows(InvalidBusinessRuleException.class, () -> policy.validateScores(
                scores, FeedbackDirection.PATIENT_TO_PRACTITIONER,
                List.of(punctuality, careInactive, clarity, satisfaction)));
    }

    @Test
    @DisplayName("rechaza scores vacíos")
    void rejectEmpty() {
        assertThrows(InvalidBusinessRuleException.class, () -> policy.validateScores(
                Map.of(), FeedbackDirection.PATIENT_TO_PRACTITIONER, activeP2P()));
    }

    @Test
    @DisplayName("rechaza si no hay criterios activos definidos para la dirección")
    void rejectNoActiveCriteria() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(FeedbackCriterionCodes.PUNCTUALITY, 5);
        assertThrows(InvalidBusinessRuleException.class, () -> policy.validateScores(
                scores, FeedbackDirection.PATIENT_TO_PRACTITIONER, List.of()));
    }

    private static List<FeedbackCriterion> activeP2P() {
        return List.of(
                new FeedbackCriterion(FeedbackCriterionCodes.PUNCTUALITY,
                        "Puntualidad", null, FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 1, true),
                new FeedbackCriterion(FeedbackCriterionCodes.CARE_QUALITY,
                        "Calidad", null, FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 2, true),
                new FeedbackCriterion(FeedbackCriterionCodes.COMMUNICATION_CLARITY,
                        "Claridad", null, FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 3, true),
                new FeedbackCriterion(FeedbackCriterionCodes.GENERAL_SATISFACTION,
                        "Satisfacción", null, FeedbackDirection.PATIENT_TO_PRACTITIONER, false, 4, true)
        );
    }
}
