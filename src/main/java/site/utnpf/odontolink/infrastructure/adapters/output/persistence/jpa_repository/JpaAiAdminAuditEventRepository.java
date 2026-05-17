package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAdminAuditEventEntity;

import java.util.List;

@Repository
public interface JpaAiAdminAuditEventRepository
        extends JpaRepository<AiAdminAuditEventEntity, Long>,
                JpaSpecificationExecutor<AiAdminAuditEventEntity> {

    List<AiAdminAuditEventEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
