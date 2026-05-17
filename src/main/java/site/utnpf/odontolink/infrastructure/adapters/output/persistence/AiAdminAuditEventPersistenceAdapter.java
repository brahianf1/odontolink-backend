package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAdminAuditEventEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAiAdminAuditEventRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AiAdminAuditEventPersistenceMapper;

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
}
