package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IFeedbackUseCase;
import site.utnpf.odontolink.application.port.in.dto.CriterionScoreInput;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackCriterionScore;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.service.FeedbackCriterionPolicyService;
import site.utnpf.odontolink.domain.service.FeedbackPolicyService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicación micro-contexto de Feedback (RF21, RF22, RF23, RF24).
 *
 * <p>Orquesta validación de reglas de negocio (estado de atención, autoría,
 * unicidad, set de criterios para la dirección) y persistencia atómica del
 * feedback con su subcolección de scores.
 */
@Transactional
public class FeedbackService implements IFeedbackUseCase {

    private final FeedbackRepository feedbackRepository;
    private final AttentionRepository attentionRepository;
    private final FeedbackCriterionRepository criterionRepository;
    private final FeedbackPolicyService feedbackPolicyService;
    private final FeedbackCriterionPolicyService criterionPolicyService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           AttentionRepository attentionRepository,
                           FeedbackCriterionRepository criterionRepository,
                           FeedbackPolicyService feedbackPolicyService,
                           FeedbackCriterionPolicyService criterionPolicyService) {
        this.feedbackRepository = feedbackRepository;
        this.attentionRepository = attentionRepository;
        this.criterionRepository = criterionRepository;
        this.feedbackPolicyService = feedbackPolicyService;
        this.criterionPolicyService = criterionPolicyService;
    }

    @Override
    public Feedback createFeedback(Long attentionId,
                                   List<CriterionScoreInput> scores,
                                   String comment,
                                   User submittingUser) {
        if (scores == null || scores.isEmpty()) {
            throw new InvalidBusinessRuleException("La encuesta requiere al menos un score.");
        }

        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attention", "id", attentionId.toString()));

        feedbackPolicyService.validateFeedbackCreation(attention, submittingUser);

        Feedback transientFeedback = new Feedback(attention, submittingUser, comment);
        FeedbackDirection direction = FeedbackDirection.of(transientFeedback);
        if (direction == null) {
            // No debería pasar: validateFeedbackCreation ya verifica pertenencia.
            throw new InvalidBusinessRuleException(
                    "No se pudo determinar la dirección del feedback.");
        }

        Map<String, Integer> scoresByCode = toScoresMap(scores);
        List<FeedbackCriterion> activeCriteria = criterionRepository.findActiveByDirection(direction);
        criterionPolicyService.validateScores(scoresByCode, direction, activeCriteria);

        Map<String, FeedbackCriterion> criteriaByCode = new LinkedHashMap<>();
        for (FeedbackCriterion c : activeCriteria) {
            criteriaByCode.put(c.getCode(), c);
        }
        for (CriterionScoreInput input : scores) {
            FeedbackCriterion criterion = criteriaByCode.get(input.getCriterionCode());
            // criterion no puede ser null acá: validateScores ya lo garantizó.
            transientFeedback.addScore(new FeedbackCriterionScore(criterion, input.getScore()));
        }

        return feedbackRepository.save(transientFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getFeedbackForAttention(Long attentionId, User requestingUser) {
        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attention", "id", attentionId.toString()));
        feedbackPolicyService.validateFeedbackAccess(attention, requestingUser);
        return feedbackRepository.findByAttention(attention);
    }

    @Override
    @Transactional(readOnly = true)
    public Feedback getFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Feedback", "id", feedbackId.toString()));
    }

    private static Map<String, Integer> toScoresMap(List<CriterionScoreInput> inputs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (CriterionScoreInput input : inputs) {
            String code = input.getCriterionCode();
            if (map.containsKey(code)) {
                throw new InvalidBusinessRuleException(
                        "Score duplicado para el criterio " + code);
            }
            map.put(code, input.getScore());
        }
        return map;
    }
}
