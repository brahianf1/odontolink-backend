package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiGovernancePolicyEntity;

@Repository
public interface JpaAiGovernancePolicyRepository extends JpaRepository<AiGovernancePolicyEntity, Long> {
}
