package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackCriterionEntity;

import java.util.List;
import java.util.Optional;

public interface JpaFeedbackCriterionRepository extends JpaRepository<FeedbackCriterionEntity, Long> {

    Optional<FeedbackCriterionEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<FeedbackCriterionEntity> findByApplicableDirectionAndActiveTrueOrderByDisplayOrderAsc(
            FeedbackDirection applicableDirection);

    List<FeedbackCriterionEntity> findByApplicableDirectionAndActiveTrueAndIncludeInRankingTrueOrderByDisplayOrderAsc(
            FeedbackDirection applicableDirection);

    List<FeedbackCriterionEntity> findAllByOrderByDisplayOrderAsc();
}
