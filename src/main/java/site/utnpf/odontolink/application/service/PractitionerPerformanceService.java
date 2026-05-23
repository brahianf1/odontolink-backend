package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase;
import site.utnpf.odontolink.application.port.in.dto.PractitionerCriterionChartQuery;
import site.utnpf.odontolink.application.port.in.dto.PractitionerRankingChartQuery;
import site.utnpf.odontolink.application.service.support.SupervisorScopeResolver;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.PractitionerCriterionPerformance;
import site.utnpf.odontolink.domain.model.PractitionerRankingEntry;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación de los charts de performance del panel docente.
 *
 * <p>Aplica el cerco supervisor→practicantes vía {@link SupervisorScopeResolver}.
 * Lee los thresholds (umbral mínimo de muestras y topN default) desde
 * configuración para que el operador pueda calibrarlos sin redeploy.
 */
@Transactional(readOnly = true)
public class PractitionerPerformanceService implements IPractitionerPerformanceUseCase {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackCriterionRepository criterionRepository;
    private final SupervisorScopeResolver scopeResolver;
    private final int minFeedbackCount;
    private final int defaultTopN;

    public PractitionerPerformanceService(FeedbackRepository feedbackRepository,
                                          FeedbackCriterionRepository criterionRepository,
                                          SupervisorScopeResolver scopeResolver,
                                          int minFeedbackCount,
                                          int defaultTopN) {
        this.feedbackRepository = feedbackRepository;
        this.criterionRepository = criterionRepository;
        this.scopeResolver = scopeResolver;
        this.minFeedbackCount = Math.max(1, minFeedbackCount);
        this.defaultTopN = Math.max(1, defaultTopN);
    }

    @Override
    public PractitionerCriterionChartResult getTopByCriterion(PractitionerCriterionChartQuery query,
                                                              User supervisorUser) {
        if (query == null || query.getCriterionCode() == null || query.getCriterionCode().isBlank()) {
            throw new IllegalArgumentException("criterionCode requerido.");
        }
        FeedbackCriterion criterion = criterionRepository.findByCode(query.getCriterionCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FeedbackCriterion", "code", query.getCriterionCode()));

        Set<Long> scope = scopeResolver.resolveAllowedPractitionerIds(supervisorUser);
        int topN = query.getTopN() != null && query.getTopN() > 0 ? query.getTopN() : defaultTopN;

        List<PractitionerCriterionPerformance> entries = scope.isEmpty()
                ? Collections.emptyList()
                : feedbackRepository.topPractitionersByCriterion(
                        criterion.getCode(),
                        scope,
                        query.getStartDate(),
                        query.getEndDate(),
                        query.getTreatmentId(),
                        minFeedbackCount,
                        topN);

        return new PractitionerCriterionChartResult(
                criterion.getCode(),
                criterion.getDisplayName(),
                minFeedbackCount,
                entries
        );
    }

    @Override
    public PractitionerRankingChartResult getOverallRanking(PractitionerRankingChartQuery query,
                                                            User supervisorUser) {
        Set<Long> scope = scopeResolver.resolveAllowedPractitionerIds(supervisorUser);
        int topN = query != null && query.getTopN() != null && query.getTopN() > 0
                ? query.getTopN()
                : defaultTopN;

        List<FeedbackCriterion> rankingCriteria = criterionRepository
                .findActiveRankingByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER);
        List<CriterionLabel> labels = rankingCriteria.stream()
                .map(c -> new CriterionLabel(c.getCode(), c.getDisplayName()))
                .collect(Collectors.toList());

        List<PractitionerRankingEntry> entries;
        if (scope.isEmpty() || rankingCriteria.isEmpty()) {
            entries = Collections.emptyList();
        } else {
            entries = feedbackRepository.practitionerOverallRanking(
                    scope,
                    query != null ? query.getStartDate() : null,
                    query != null ? query.getEndDate() : null,
                    query != null ? query.getTreatmentId() : null,
                    minFeedbackCount,
                    topN
            );
        }

        return new PractitionerRankingChartResult(labels, minFeedbackCount, entries);
    }
}
