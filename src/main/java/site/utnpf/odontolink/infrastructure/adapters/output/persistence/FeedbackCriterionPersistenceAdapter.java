package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaFeedbackCriterionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.FeedbackCriterionPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class FeedbackCriterionPersistenceAdapter implements FeedbackCriterionRepository {

    private final JpaFeedbackCriterionRepository jpaRepository;

    public FeedbackCriterionPersistenceAdapter(JpaFeedbackCriterionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<FeedbackCriterion> findActiveByDirection(FeedbackDirection direction) {
        return jpaRepository
                .findByApplicableDirectionAndActiveTrueOrderByDisplayOrderAsc(direction)
                .stream()
                .map(FeedbackCriterionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeedbackCriterion> findActiveRankingByDirection(FeedbackDirection direction) {
        return jpaRepository
                .findByApplicableDirectionAndActiveTrueAndIncludeInRankingTrueOrderByDisplayOrderAsc(direction)
                .stream()
                .map(FeedbackCriterionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeedbackCriterion> findAll() {
        return jpaRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(FeedbackCriterionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FeedbackCriterion> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(FeedbackCriterionPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    @Transactional
    public FeedbackCriterion save(FeedbackCriterion criterion) {
        var entity = FeedbackCriterionPersistenceMapper.toEntity(criterion);
        var saved = jpaRepository.save(entity);
        return FeedbackCriterionPersistenceMapper.toDomain(saved);
    }
}
