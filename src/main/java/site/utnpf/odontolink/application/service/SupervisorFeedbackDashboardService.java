package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISupervisorFeedbackDashboardUseCase;
import site.utnpf.odontolink.application.port.in.dto.SupervisorFeedbackDashboardQuery;
import site.utnpf.odontolink.application.service.support.SupervisorScopeResolver;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;
import site.utnpf.odontolink.domain.model.FeedbackDirectionalAggregates;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;

import java.util.Collections;
import java.util.Set;

/**
 * Servicio de aplicación del Panel Docente de Supervisión de Feedback (RF25).
 *
 * <p>El cerco supervisor→practicantes se resuelve via
 * {@link SupervisorScopeResolver} (helper compartido con los charts del
 * panel docente). Mantenemos en este servicio sólo la composición específica
 * del dashboard (paginado + agregados por dirección).
 */
@Transactional(readOnly = true)
public class SupervisorFeedbackDashboardService implements ISupervisorFeedbackDashboardUseCase {

    private final FeedbackRepository feedbackRepository;
    private final SupervisorScopeResolver scopeResolver;

    public SupervisorFeedbackDashboardService(FeedbackRepository feedbackRepository,
                                              SupervisorScopeResolver scopeResolver) {
        this.feedbackRepository = feedbackRepository;
        this.scopeResolver = scopeResolver;
    }

    @Override
    public FeedbackDashboardResult getDashboard(SupervisorFeedbackDashboardQuery query,
                                                PageQuery pageQuery,
                                                User supervisorUser) {
        Set<Long> allowedPractitionerIds = scopeResolver.resolveAllowedPractitionerIds(supervisorUser);
        scopeResolver.rejectIfOutOfScope(query.getPractitionerId(), allowedPractitionerIds);

        if (allowedPractitionerIds.isEmpty()) {
            PageResult<Feedback> emptyPage = new PageResult<>(
                    Collections.emptyList(),
                    pageQuery.getPage(),
                    pageQuery.getSize(),
                    0L,
                    0
            );
            return new FeedbackDashboardResult(emptyPage, FeedbackDirectionalAggregates.empty());
        }

        FeedbackSearchCriteria criteria = new FeedbackSearchCriteria(
                query.getPractitionerId(),
                query.getPatientId(),
                query.getTreatmentId(),
                query.getStartDate(),
                query.getEndDate(),
                query.getDirection(),
                allowedPractitionerIds
        );

        PageResult<Feedback> page = feedbackRepository.searchDashboard(criteria, pageQuery);
        FeedbackDirectionalAggregates aggregates = feedbackRepository.aggregateByDirection(criteria);

        return new FeedbackDashboardResult(page, aggregates);
    }
}
