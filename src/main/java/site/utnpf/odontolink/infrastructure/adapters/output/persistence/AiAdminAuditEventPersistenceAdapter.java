package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAdminAuditEventEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAiAdminAuditEventRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AiAdminAuditEventPersistenceMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@Transactional(readOnly = true)
public class AiAdminAuditEventPersistenceAdapter implements AiAdminAuditEventRepository {

    private final JpaAiAdminAuditEventRepository jpa;

    public AiAdminAuditEventPersistenceAdapter(JpaAiAdminAuditEventRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public AiAdminAuditEvent save(AiAdminAuditEvent event) {
        AiAdminAuditEventEntity entity = AiAdminAuditEventPersistenceMapper.toEntity(event);
        AiAdminAuditEventEntity saved = jpa.save(entity);
        return AiAdminAuditEventPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<AiAdminAuditEvent> findAllOrderByOccurredAtDesc(int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 500));
        return jpa.findAllByOrderByOccurredAtDesc(PageRequest.of(0, effectiveLimit)).stream()
                .map(AiAdminAuditEventPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<AiAdminAuditEvent> findPaged(AiAdminAuditEvent.Type type,
                                                   Instant from,
                                                   Instant to,
                                                   int page,
                                                   int size) {
        Specification<AiAdminAuditEventEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                // Filtro half-open: from inclusivo, to exclusivo. Es la
                // semantica esperada por la mayoria de selectores de rango.
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AiAdminAuditEventEntity> result = jpa.findAll(spec, pageRequest);
        List<AiAdminAuditEvent> content = result.getContent().stream()
                .map(AiAdminAuditEventPersistenceMapper::toDomain)
                .toList();
        return new PageResult<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
