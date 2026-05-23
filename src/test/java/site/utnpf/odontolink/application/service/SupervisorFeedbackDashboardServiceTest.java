package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.utnpf.odontolink.application.port.in.dto.SupervisorFeedbackDashboardQuery;
import site.utnpf.odontolink.application.service.support.SupervisorScopeResolver;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.FeedbackDirectionalAggregates;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupervisorFeedbackDashboardServiceTest {

    private FeedbackRepository feedbackRepository;
    private SupervisorRepository supervisorRepository;
    private SupervisorFeedbackDashboardService service;

    @BeforeEach
    void setUp() {
        feedbackRepository = mock(FeedbackRepository.class);
        supervisorRepository = mock(SupervisorRepository.class);
        SupervisorScopeResolver resolver = new SupervisorScopeResolver(supervisorRepository);
        service = new SupervisorFeedbackDashboardService(feedbackRepository, resolver);
    }

    @Test
    @DisplayName("agregados directionales viajan tal cual al resultado")
    void aggregatesAreSurfacedToCaller() {
        Supervisor sup = supervisorWithPractitioners(11L, 22L);
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(sup));

        PageResult<Feedback> page = new PageResult<>(List.of(), 0, 20, 0L, 0);
        when(feedbackRepository.searchDashboard(any(), any())).thenReturn(page);
        FeedbackDirectionalAggregates aggregates = new FeedbackDirectionalAggregates(
                4.5, 12L, 3.2, 5L);
        when(feedbackRepository.aggregateByDirection(any())).thenReturn(aggregates);

        SupervisorFeedbackDashboardQuery query = new SupervisorFeedbackDashboardQuery(
                null, null, null, null, null, null);
        FeedbackDashboardResult result = service.getDashboard(
                query, PageQuery.of(0, 20, null, null), supervisorUser(7L));

        assertEquals(4.5, result.getAggregates().getAverageRatingPatientToPractitioner());
        assertEquals(12L, result.getAggregates().getTotalPatientToPractitioner());
        assertEquals(3.2, result.getAggregates().getAverageRatingPractitionerToPatient());
        assertEquals(5L, result.getAggregates().getTotalPractitionerToPatient());
    }

    @Test
    @DisplayName("filtro direction se propaga al criteria del repositorio")
    void directionPropagatesToCriteria() {
        Supervisor sup = supervisorWithPractitioners(11L);
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(sup));
        when(feedbackRepository.searchDashboard(any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));
        when(feedbackRepository.aggregateByDirection(any()))
                .thenReturn(FeedbackDirectionalAggregates.empty());

        SupervisorFeedbackDashboardQuery query = new SupervisorFeedbackDashboardQuery(
                null, null, null, null, null, FeedbackDirection.PATIENT_TO_PRACTITIONER);
        service.getDashboard(query, PageQuery.of(0, 20, null, null), supervisorUser(7L));

        ArgumentCaptor<FeedbackSearchCriteria> captor =
                ArgumentCaptor.forClass(FeedbackSearchCriteria.class);
        verify(feedbackRepository).searchDashboard(captor.capture(), any());

        FeedbackSearchCriteria sent = captor.getValue();
        assertEquals(FeedbackDirection.PATIENT_TO_PRACTITIONER, sent.getDirection());
        assertEquals(Set.of(11L), sent.getAllowedPractitionerIds());
    }

    @Test
    @DisplayName("supervisor sin practicantes obtiene agregados vacíos sin tocar el repo")
    void emptyScopeShortCircuits() {
        Supervisor sup = supervisorWithPractitioners();
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(sup));

        FeedbackDashboardResult result = service.getDashboard(
                new SupervisorFeedbackDashboardQuery(null, null, null, null, null, null),
                PageQuery.of(0, 20, null, null),
                supervisorUser(7L));

        assertEquals(0.0, result.getAggregates().getAverageRatingPatientToPractitioner());
        assertEquals(0L, result.getAggregates().getTotalPatientToPractitioner());
        assertEquals(0.0, result.getAggregates().getAverageRatingPractitionerToPatient());
        assertEquals(0L, result.getAggregates().getTotalPractitionerToPatient());
        verify(feedbackRepository, never()).searchDashboard(any(), any());
        verify(feedbackRepository, never()).aggregateByDirection(any());
    }

    @Test
    @DisplayName("practicante fuera del cerco devuelve 403 (UnauthorizedOperation)")
    void outOfScopePractitionerRejected() {
        Supervisor sup = supervisorWithPractitioners(11L);
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(sup));

        SupervisorFeedbackDashboardQuery query = new SupervisorFeedbackDashboardQuery(
                99L, null, null, null, null, null);
        assertThrows(UnauthorizedOperationException.class,
                () -> service.getDashboard(query, PageQuery.of(0, 20, null, null), supervisorUser(7L)));
        verify(feedbackRepository, never()).searchDashboard(any(), any());
        verify(feedbackRepository, never()).aggregateByDirection(any());
    }

    @Test
    @DisplayName("usuario sin rol SUPERVISOR es rechazado sin consultar el repo")
    void nonSupervisorRejected() {
        User patient = new User();
        patient.setId(99L);
        patient.setRole(Role.ROLE_PATIENT);

        assertThrows(UnauthorizedOperationException.class,
                () -> service.getDashboard(
                        new SupervisorFeedbackDashboardQuery(null, null, null, null, null, null),
                        PageQuery.of(0, 20, null, null), patient));
        verify(supervisorRepository, times(0)).findByUserId(any());
    }

    private Supervisor supervisorWithPractitioners(Long... practitionerIds) {
        Supervisor supervisor = new Supervisor();
        supervisor.setId(1L);
        java.util.Set<Practitioner> set = new java.util.HashSet<>();
        for (Long id : practitionerIds) {
            Practitioner p = mock(Practitioner.class);
            when(p.getId()).thenReturn(id);
            set.add(p);
        }
        supervisor.setSupervisedPractitioners(set.isEmpty() ? Collections.emptySet() : set);
        return supervisor;
    }

    private User supervisorUser(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.ROLE_SUPERVISOR);
        return u;
    }
}
