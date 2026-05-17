package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiGovernancePolicyEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAiGovernancePolicyRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AiGovernancePolicyPersistenceMapper;

import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class AiGovernancePolicyPersistenceAdapter implements AiGovernancePolicyRepository {

    private final JpaAiGovernancePolicyRepository jpa;

    public AiGovernancePolicyPersistenceAdapter(JpaAiGovernancePolicyRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AiGovernancePolicy> findSingleton() {
        return jpa.findById(AiGovernancePolicy.SINGLETON_ID)
                .map(AiGovernancePolicyPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public AiGovernancePolicy save(AiGovernancePolicy policy) {
        // Defensa en profundidad: el id del singleton se re-fuerza aqui
        // para que ninguna corrupcion pueda crear filas paralelas.
        policy.setId(AiGovernancePolicy.SINGLETON_ID);
        AiGovernancePolicyEntity entity = AiGovernancePolicyPersistenceMapper.toEntity(policy);
        AiGovernancePolicyEntity saved = jpa.save(entity);
        return AiGovernancePolicyPersistenceMapper.toDomain(saved);
    }
}
