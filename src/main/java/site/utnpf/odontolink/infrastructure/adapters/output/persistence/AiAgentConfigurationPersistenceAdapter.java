package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAiAgentConfigurationRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AiAgentConfigurationPersistenceMapper;

import java.util.Optional;

/**
 * Adaptador de persistencia para {@link AiAgentConfiguration} (RF31, RF32).
 *
 * <p>Mismo patron singleton que {@link InstitutionalSettingsPersistenceAdapter}:
 * todas las operaciones operan sobre el id fijo del agregado y la operacion
 * de guardado re-fuerza el SINGLETON_ID antes de persistir.
 */
@Component
@Transactional(readOnly = true)
public class AiAgentConfigurationPersistenceAdapter implements AiAgentConfigurationRepository {

    private final JpaAiAgentConfigurationRepository jpaRepository;

    public AiAgentConfigurationPersistenceAdapter(JpaAiAgentConfigurationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AiAgentConfiguration> findSingleton() {
        return jpaRepository.findById(AiAgentConfiguration.SINGLETON_ID)
                .map(AiAgentConfigurationPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public AiAgentConfiguration save(AiAgentConfiguration configuration) {
        // Defensa en profundidad: aunque defaults() y el caller asignen el
        // SINGLETON_ID, re-forzamos aqui para garantizar la unicidad incluso
        // ante futuros refactors.
        configuration.setId(AiAgentConfiguration.SINGLETON_ID);
        AiAgentConfigurationEntity entity = AiAgentConfigurationPersistenceMapper.toEntity(configuration);
        AiAgentConfigurationEntity saved = jpaRepository.save(entity);
        return AiAgentConfigurationPersistenceMapper.toDomain(saved);
    }
}
