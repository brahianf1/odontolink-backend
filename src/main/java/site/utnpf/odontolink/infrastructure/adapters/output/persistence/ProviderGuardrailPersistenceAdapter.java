package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.ProviderGuardrail;
import site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ProviderGuardrailEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaProviderGuardrailRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ProviderGuardrailPersistenceMapper;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class ProviderGuardrailPersistenceAdapter implements ProviderGuardrailRepository {

    private final JpaProviderGuardrailRepository jpa;

    public ProviderGuardrailPersistenceAdapter(JpaProviderGuardrailRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ProviderGuardrail> findAllOrderByPriorityAsc() {
        return jpa.findAllByOrderByPriorityAscIdAsc().stream()
                .map(ProviderGuardrailPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProviderGuardrail> findAllAttachedOrderByPriorityAsc() {
        return jpa.findByAttachedTrueOrderByPriorityAscIdAsc().stream()
                .map(ProviderGuardrailPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProviderGuardrail> findById(Long id) {
        return jpa.findById(id).map(ProviderGuardrailPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ProviderGuardrail> findByProviderGuardrailUuid(String uuid) {
        return jpa.findByProviderGuardrailUuid(uuid)
                .map(ProviderGuardrailPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public ProviderGuardrail save(ProviderGuardrail guardrail) {
        ProviderGuardrailEntity entity = ProviderGuardrailPersistenceMapper.toEntity(guardrail);
        ProviderGuardrailEntity saved = jpa.save(entity);
        return ProviderGuardrailPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
