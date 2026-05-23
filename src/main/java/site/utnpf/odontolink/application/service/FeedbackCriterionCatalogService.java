package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IFeedbackCriterionCatalogUseCase;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;

import java.util.List;

@Transactional(readOnly = true)
public class FeedbackCriterionCatalogService implements IFeedbackCriterionCatalogUseCase {

    private final FeedbackCriterionRepository criterionRepository;

    public FeedbackCriterionCatalogService(FeedbackCriterionRepository criterionRepository) {
        this.criterionRepository = criterionRepository;
    }

    @Override
    public List<FeedbackCriterion> listActiveForDirection(FeedbackDirection direction) {
        return criterionRepository.findActiveByDirection(direction);
    }
}
