package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaFeedbackRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.FeedbackPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Feedback (Hexagonal Architecture).
 * Implementa la interfaz del dominio FeedbackRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Este adaptador maneja la persistencia de feedbacks y proporciona métodos
 * de consulta para los diferentes casos de uso del sistema.
 *
 * @author OdontoLink Team
 */
@Component
public class FeedbackPersistenceAdapter implements FeedbackRepository {

    private final JpaFeedbackRepository jpaFeedbackRepository;

    public FeedbackPersistenceAdapter(JpaFeedbackRepository jpaFeedbackRepository) {
        this.jpaFeedbackRepository = jpaFeedbackRepository;
    }

    @Override
    public Feedback save(Feedback feedback) {
        var entity = FeedbackPersistenceMapper.toEntity(feedback);
        var savedEntity = jpaFeedbackRepository.save(entity);
        return FeedbackPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Feedback> findById(Long id) {
        return jpaFeedbackRepository.findById(id)
                .map(FeedbackPersistenceMapper::toDomain);
    }

    @Override
    public List<Feedback> findByAttention(Attention attention) {
        return findByAttentionId(attention.getId());
    }

    @Override
    public List<Feedback> findByAttentionId(Long attentionId) {
        return jpaFeedbackRepository.findByAttention_Id(attentionId).stream()
                .map(FeedbackPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAttentionAndSubmittedBy(Attention attention, User submittedBy) {
        return jpaFeedbackRepository.existsByAttention_IdAndSubmittedBy_Id(
                attention.getId(),
                submittedBy.getId()
        );
    }

    @Override
    public List<Feedback> findByPractitionerId(Long practitionerId) {
        return jpaFeedbackRepository.findByPractitionerId(practitionerId).stream()
                .map(FeedbackPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Feedback> findBySubmittedById(Long userId) {
        return jpaFeedbackRepository.findBySubmittedBy_Id(userId).stream()
                .map(FeedbackPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Feedback> findByAttentionIdAndSubmittedById(Long attentionId, Long userId) {
        return jpaFeedbackRepository.findByAttention_IdAndSubmittedBy_Id(attentionId, userId)
                .map(FeedbackPersistenceMapper::toDomain);
    }
}
