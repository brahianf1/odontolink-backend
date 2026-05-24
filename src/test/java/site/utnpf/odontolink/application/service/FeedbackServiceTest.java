package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.application.port.in.dto.CriterionScoreInput;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.AttentionStatus;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackCriterionCodes;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.service.FeedbackCriterionPolicyService;
import site.utnpf.odontolink.domain.service.FeedbackPolicyService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private FeedbackRepository feedbackRepository;
    private AttentionRepository attentionRepository;
    private FeedbackCriterionRepository criterionRepository;
    private FeedbackPolicyService feedbackPolicyService;
    private FeedbackService service;

    @BeforeEach
    void setUp() {
        feedbackRepository = mock(FeedbackRepository.class);
        attentionRepository = mock(AttentionRepository.class);
        criterionRepository = mock(FeedbackCriterionRepository.class);
        feedbackPolicyService = mock(FeedbackPolicyService.class);
        service = new FeedbackService(
                feedbackRepository, attentionRepository, criterionRepository,
                feedbackPolicyService, new FeedbackCriterionPolicyService());
    }

    @Test
    @DisplayName("createFeedback con scores válidos persiste feedback con subcolección")
    void happyPath() {
        Attention attention = buildAttention(100L, AttentionStatus.COMPLETED,
                /*patientUserId*/ 1L, /*practitionerUserId*/ 2L);
        when(attentionRepository.findById(100L)).thenReturn(Optional.of(attention));
        when(criterionRepository.findActiveByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER))
                .thenReturn(activeP2P());
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(inv -> {
                    Feedback fb = inv.getArgument(0);
                    fb.setId(500L);
                    return fb;
                });

        User patient = userOf(1L, Role.ROLE_PATIENT);
        Feedback result = service.createFeedback(100L,
                List.of(
                        new CriterionScoreInput(FeedbackCriterionCodes.PUNCTUALITY, 5),
                        new CriterionScoreInput(FeedbackCriterionCodes.CARE_QUALITY, 4),
                        new CriterionScoreInput(FeedbackCriterionCodes.COMMUNICATION_CLARITY, 5),
                        new CriterionScoreInput(FeedbackCriterionCodes.GENERAL_SATISFACTION, 5)),
                "Excelente",
                patient);

        assertEquals(500L, result.getId());
        assertEquals(4, result.getScores().size());
        verify(feedbackPolicyService).validateFeedbackCreation(attention, patient);
    }

    @Test
    @DisplayName("createFeedback con scores vacíos → 400")
    void rejectEmptyScores() {
        assertThrows(InvalidBusinessRuleException.class,
                () -> service.createFeedback(100L, List.of(), null, userOf(1L, Role.ROLE_PATIENT)));
        verify(attentionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createFeedback con attention inexistente → 404")
    void rejectMissingAttention() {
        when(attentionRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.createFeedback(404L,
                        List.of(new CriterionScoreInput(FeedbackCriterionCodes.PUNCTUALITY, 5)),
                        null,
                        userOf(1L, Role.ROLE_PATIENT)));
    }

    @Test
    @DisplayName("createFeedback con scores faltantes (3 de 4) → InvalidBusinessRule")
    void rejectIncompleteScores() {
        Attention attention = buildAttention(100L, AttentionStatus.COMPLETED, 1L, 2L);
        when(attentionRepository.findById(100L)).thenReturn(Optional.of(attention));
        when(criterionRepository.findActiveByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER))
                .thenReturn(activeP2P());

        assertThrows(InvalidBusinessRuleException.class,
                () -> service.createFeedback(100L,
                        List.of(
                                new CriterionScoreInput(FeedbackCriterionCodes.PUNCTUALITY, 5),
                                new CriterionScoreInput(FeedbackCriterionCodes.CARE_QUALITY, 4),
                                new CriterionScoreInput(FeedbackCriterionCodes.COMMUNICATION_CLARITY, 5)
                                // falta GENERAL_SATISFACTION
                        ),
                        null,
                        userOf(1L, Role.ROLE_PATIENT)));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("createFeedback rechaza scores duplicados antes de validar contra el catálogo")
    void rejectDuplicateScores() {
        Attention attention = buildAttention(100L, AttentionStatus.COMPLETED, 1L, 2L);
        when(attentionRepository.findById(100L)).thenReturn(Optional.of(attention));

        assertThrows(InvalidBusinessRuleException.class,
                () -> service.createFeedback(100L,
                        List.of(
                                new CriterionScoreInput(FeedbackCriterionCodes.PUNCTUALITY, 5),
                                new CriterionScoreInput(FeedbackCriterionCodes.PUNCTUALITY, 4)
                        ),
                        null,
                        userOf(1L, Role.ROLE_PATIENT)));
        verify(feedbackRepository, never()).save(any());
    }

    private static List<FeedbackCriterion> activeP2P() {
        return List.of(
                new FeedbackCriterion(FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad", null,
                        FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 1, true),
                new FeedbackCriterion(FeedbackCriterionCodes.CARE_QUALITY, "Calidad", null,
                        FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 2, true),
                new FeedbackCriterion(FeedbackCriterionCodes.COMMUNICATION_CLARITY, "Claridad", null,
                        FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 3, true),
                new FeedbackCriterion(FeedbackCriterionCodes.GENERAL_SATISFACTION, "Satisfacción", null,
                        FeedbackDirection.PATIENT_TO_PRACTITIONER, false, 4, true)
        );
    }

    private static Attention buildAttention(long id, AttentionStatus status,
                                            long patientUserId, long practitionerUserId) {
        User patientUser = userOf(patientUserId, Role.ROLE_PATIENT);
        User practitionerUser = userOf(practitionerUserId, Role.ROLE_PRACTITIONER);

        Patient patient = new Patient();
        patient.setId(50L);
        patient.setUser(patientUser);

        Practitioner practitioner = new Practitioner();
        practitioner.setId(60L);
        practitioner.setUser(practitionerUser);

        Attention attention = new Attention();
        attention.setId(id);
        attention.setPatient(patient);
        attention.setPractitioner(practitioner);
        attention.setStatus(status);
        return attention;
    }

    private static User userOf(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }
}
