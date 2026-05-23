package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackCriterionCodes;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackCriterionCatalogServiceTest {

    @Test
    void delegatesToRepositoryFilteredByDirection() {
        FeedbackCriterionRepository repo = mock(FeedbackCriterionRepository.class);
        FeedbackCriterion crit = new FeedbackCriterion(
                FeedbackCriterionCodes.PUNCTUALITY, "Puntualidad", null,
                FeedbackDirection.PATIENT_TO_PRACTITIONER, true, 1, true);
        when(repo.findActiveByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER))
                .thenReturn(List.of(crit));

        FeedbackCriterionCatalogService service = new FeedbackCriterionCatalogService(repo);
        List<FeedbackCriterion> result =
                service.listActiveForDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER);

        assertEquals(1, result.size());
        assertEquals(FeedbackCriterionCodes.PUNCTUALITY, result.get(0).getCode());
        verify(repo).findActiveByDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER);
    }
}
