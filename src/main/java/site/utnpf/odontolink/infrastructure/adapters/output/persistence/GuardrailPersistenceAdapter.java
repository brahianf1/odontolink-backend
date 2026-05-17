package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.domain.repository.GuardrailRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.GuardrailEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaGuardrailRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.GuardrailPersistenceMapper;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class GuardrailPersistenceAdapter implements GuardrailRepository {

    private final JpaGuardrailRepository jpa;

    public GuardrailPersistenceAdapter(JpaGuardrailRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Guardrail> findAllOrderByCreatedAtAsc() {
        return jpa.findAllByOrderByCreatedAtAsc().stream()
                .map(GuardrailPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Guardrail> findAllActiveOrderByCreatedAtAsc() {
        return jpa.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(GuardrailPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Guardrail> findById(Long id) {
        return jpa.findById(id).map(GuardrailPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Guardrail save(Guardrail guardrail) {
        GuardrailEntity entity = GuardrailPersistenceMapper.toEntity(guardrail);
        GuardrailEntity saved = jpa.save(entity);
        return GuardrailPersistenceMapper.toDomain(saved);
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
