package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase.PractitionerCriterionChartResult;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase.PractitionerRankingChartResult;
import site.utnpf.odontolink.application.port.in.dto.PractitionerCriterionChartQuery;
import site.utnpf.odontolink.application.port.in.dto.PractitionerRankingChartQuery;
import site.utnpf.odontolink.application.service.support.SupervisorScopeResolver;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackCriterionCodes;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.PractitionerCriterionPerformance;
import site.utnpf.odontolink.domain.model.PractitionerRankingEntry;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PractitionerPerformanceServiceTest {

    private static final int MIN_SAMPLES = 3;
    private static final int DEFAULT_TOP_N = 10;

    private FeedbackRepository feedbackRepository;
    private FeedbackCriterionRepository criterionRepository;
    private SupervisorRepository supervisorRepository;
    private PractitionerPerformanceService service;

    @BeforeEach
    void setUp() {
        feedbackRepository = mock(FeedbackRepository.class);
        criterionRepository = mock(FeedbackCriterionRepository.class);
        supervisorRepository = mock(SupervisorRepository.class);
        SupervisorScopeResolver resolver = new SupervisorScopeResolver(supervisorRepository);
        service = new PractitionerPerformanceService(
                feedbackRepository, criterionRepository, resolver, MIN_SAMPLES, DEFAULT_TOP_N);
    }

    @Test
    @DisplayName("topByCriterion: criterion desconocido → ResourceNotFoundException")
    void topByCriterion_unknownCriterion() {
        when(criterionRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());
        PractitionerCriterionChartQuery query = new PractitionerCriterionChartQuery(
                "UNKNOWN", null, null, null, null);
        assertThrows(ResourceNotFoundException.class,
                () -> service.getTopByCriterion(query, supervisorUser(7L)));
    }

    @Test
    @DisplayName("topByCriterion: criterio inactivo → ResourceNotFoundException (no zombie chart)")
    void topByCriterion_inactiveCriterion() {
        FeedbackCriterion inactive = new FeedbackCriterion(
                FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 1, false);
        when(criterionRepository.findByCode(FeedbackCriterionCodes.PUNCTUALITY))
                .thenReturn(Optional.of(inactive));

        PractitionerCriterionChartQuery query = new PractitionerCriterionChartQuery(
                FeedbackCriterionCodes.PUNCTUALITY, null, null, null, null);
        assertThrows(ResourceNotFoundException.class,
                () -> service.getTopByCriterion(query, supervisorUser(7L)));
        verify(feedbackRepository, never()).topPractitionersByCriterion(
                anyString(), any(), any(), any(), any(), anyInt(), anyInt());
        verify(supervisorRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("topByCriterion: cerco vacío devuelve entries=[] sin consultar feedback repo")
    void topByCriterion_emptyScopeShortCircuit() {
        when(criterionRepository.findByCode(FeedbackCriterionCodes.PUNCTUALITY))
                .thenReturn(Optional.of(criterion(FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad")));
        site.utnpf.odontolink.domain.model.Supervisor sup = new site.utnpf.odontolink.domain.model.Supervisor();
        sup.setId(1L);
        sup.setSupervisedPractitioners(java.util.Collections.emptySet());
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(sup));

        PractitionerCriterionChartQuery query = new PractitionerCriterionChartQuery(
                FeedbackCriterionCodes.PUNCTUALITY, null, null, null, null);
        PractitionerCriterionChartResult result = service.getTopByCriterion(query, supervisorUser(7L));

        assertTrue(result.getEntries().isEmpty());
        assertEquals(MIN_SAMPLES, result.getMinSamplesThreshold());
        verify(feedbackRepository, never()).topPractitionersByCriterion(
                anyString(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("topByCriterion: aplica topN default cuando query no lo trae")
    void topByCriterion_defaultTopN() {
        var supervisor = supervisorWith(11L, 22L);
        when(criterionRepository.findByCode(FeedbackCriterionCodes.PUNCTUALITY))
                .thenReturn(Optional.of(criterion(FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad")));
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(supervisor));
        when(feedbackRepository.topPractitionersByCriterion(
                anyString(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new PractitionerCriterionPerformance(11L, "Ana Martínez",
                                FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad",
                                4.83, 12L, 1)
                ));

        PractitionerCriterionChartQuery query = new PractitionerCriterionChartQuery(
                FeedbackCriterionCodes.PUNCTUALITY, null, null, null, null);
        PractitionerCriterionChartResult result = service.getTopByCriterion(query, supervisorUser(7L));

        ArgumentCaptor<Integer> topCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> minCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(feedbackRepository).topPractitionersByCriterion(
                eq(FeedbackCriterionCodes.PUNCTUALITY),
                eq(Set.of(11L, 22L)),
                any(), any(), any(),
                minCaptor.capture(),
                topCaptor.capture());
        assertEquals(DEFAULT_TOP_N, topCaptor.getValue().intValue());
        assertEquals(MIN_SAMPLES, minCaptor.getValue().intValue());
        assertEquals(1, result.getEntries().size());
    }

    @Test
    @DisplayName("overallRanking: cerco vacío o sin criterios de ranking devuelve lista vacía")
    void overallRanking_emptyShortCircuit() {
        when(supervisorRepository.findByUserId(7L))
                .thenReturn(Optional.of(supervisorWith()));
        when(criterionRepository.findActiveRankingByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER))
                .thenReturn(List.of(
                        criterion(FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad")));

        PractitionerRankingChartResult result = service.getOverallRanking(
                new PractitionerRankingChartQuery(null, null, null, null), supervisorUser(7L));

        assertTrue(result.getEntries().isEmpty());
        assertEquals(1, result.getCriteriaUsed().size());
        verify(feedbackRepository, never()).practitionerOverallRanking(
                any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("overallRanking: respeta scope, defaultTopN y propaga el threshold de muestras")
    void overallRanking_happyPath() {
        var supervisor = supervisorWith(11L, 22L);
        when(supervisorRepository.findByUserId(7L)).thenReturn(Optional.of(supervisor));
        when(criterionRepository.findActiveRankingByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER))
                .thenReturn(List.of(
                        criterion(FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad"),
                        criterion(FeedbackCriterionCodes.CARE_QUALITY, "Calidad"),
                        criterion(FeedbackCriterionCodes.COMMUNICATION_CLARITY, "Claridad")));
        when(feedbackRepository.practitionerOverallRanking(
                any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new PractitionerRankingEntry(11L, "Ana Martínez", 4.7,
                                Map.of("PUNCTUALITY", 4.8, "CARE_QUALITY", 4.6, "COMMUNICATION_CLARITY", 4.7),
                                12L, 1)));

        PractitionerRankingChartResult result = service.getOverallRanking(
                new PractitionerRankingChartQuery(null, null, null, null), supervisorUser(7L));

        ArgumentCaptor<Set<Long>> scopeCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Integer> minCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> topCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(feedbackRepository).practitionerOverallRanking(
                scopeCaptor.capture(), any(), any(), any(),
                minCaptor.capture(), topCaptor.capture());
        assertEquals(Set.of(11L, 22L), scopeCaptor.getValue());
        assertEquals(MIN_SAMPLES, minCaptor.getValue().intValue());
        assertEquals(DEFAULT_TOP_N, topCaptor.getValue().intValue());
        assertEquals(3, result.getCriteriaUsed().size());
        assertEquals(1, result.getEntries().size());
    }

    @Test
    @DisplayName("usuario no-supervisor es rechazado antes de tocar el repo")
    void nonSupervisorRejected() {
        User patient = new User();
        patient.setId(99L);
        patient.setRole(Role.ROLE_PATIENT);

        assertThrows(UnauthorizedOperationException.class,
                () -> service.getOverallRanking(
                        new PractitionerRankingChartQuery(null, null, null, null), patient));
        verify(feedbackRepository, never()).practitionerOverallRanking(
                any(), any(), any(), any(), anyInt(), anyInt());
    }

    private static User supervisorUser(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.ROLE_SUPERVISOR);
        return u;
    }

    private static FeedbackCriterion criterion(String code, String displayName) {
        return new FeedbackCriterion(code, displayName, null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 1, true);
    }

    private static site.utnpf.odontolink.domain.model.Supervisor supervisorWith(Long... ids) {
        site.utnpf.odontolink.domain.model.Supervisor s =
                new site.utnpf.odontolink.domain.model.Supervisor();
        s.setId(1L);
        java.util.Set<site.utnpf.odontolink.domain.model.Practitioner> set = new java.util.HashSet<>();
        for (Long id : ids) {
            site.utnpf.odontolink.domain.model.Practitioner p =
                    mock(site.utnpf.odontolink.domain.model.Practitioner.class);
            when(p.getId()).thenReturn(id);
            set.add(p);
        }
        s.setSupervisedPractitioners(set);
        return s;
    }
}
