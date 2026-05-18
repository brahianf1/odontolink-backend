package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AgentPolicyRuleEntity;

import java.util.List;

@Repository
public interface JpaAgentPolicyRuleRepository extends JpaRepository<AgentPolicyRuleEntity, Long> {

    List<AgentPolicyRuleEntity> findAllByOrderByCreatedAtAsc();

    List<AgentPolicyRuleEntity> findByActiveTrueOrderByCreatedAtAsc();

    long countByActiveTrue();
}
