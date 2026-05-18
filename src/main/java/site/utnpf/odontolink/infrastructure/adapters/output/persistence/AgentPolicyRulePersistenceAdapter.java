package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.domain.repository.AgentPolicyRuleRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AgentPolicyRuleEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAgentPolicyRuleRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AgentPolicyRulePersistenceMapper;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class AgentPolicyRulePersistenceAdapter implements AgentPolicyRuleRepository {

    private final JpaAgentPolicyRuleRepository jpa;

    public AgentPolicyRulePersistenceAdapter(JpaAgentPolicyRuleRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<AgentPolicyRule> findAllOrderByCreatedAtAsc() {
        return jpa.findAllByOrderByCreatedAtAsc().stream()
                .map(AgentPolicyRulePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<AgentPolicyRule> findAllActiveOrderByCreatedAtAsc() {
        return jpa.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(AgentPolicyRulePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AgentPolicyRule> findById(Long id) {
        return jpa.findById(id).map(AgentPolicyRulePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public AgentPolicyRule save(AgentPolicyRule rule) {
        AgentPolicyRuleEntity entity = AgentPolicyRulePersistenceMapper.toEntity(rule);
        AgentPolicyRuleEntity saved = jpa.save(entity);
        return AgentPolicyRulePersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public long countActive() {
        return jpa.countByActiveTrue();
    }
}
