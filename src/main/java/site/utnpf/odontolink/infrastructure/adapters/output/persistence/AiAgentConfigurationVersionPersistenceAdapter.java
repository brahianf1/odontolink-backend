package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationVersionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AiAgentConfigurationVersionPersistenceMapper;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class AiAgentConfigurationVersionPersistenceAdapter implements AiAgentConfigurationVersionRepository {

    private final JpaAiAgentConfigurationVersionRepository jpa;

    public AiAgentConfigurationVersionPersistenceAdapter(JpaAiAgentConfigurationVersionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public AiAgentConfigurationVersion save(AiAgentConfigurationVersion version) {
        AiAgentConfigurationVersionEntity entity =
                AiAgentConfigurationVersionPersistenceMapper.toEntity(version);
        AiAgentConfigurationVersionEntity saved = jpa.save(entity);
        return AiAgentConfigurationVersionPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<AiAgentConfigurationVersion> findAllOrderByVersionNumberDesc() {
        return jpa.findAllByOrderByVersionNumberDesc().stream()
                .map(AiAgentConfigurationVersionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AiAgentConfigurationVersion> findByVersionNumber(int versionNumber) {
        return jpa.findByVersionNumber(versionNumber)
                .map(AiAgentConfigurationVersionPersistenceMapper::toDomain);
    }

    @Override
    public int findMaxVersionNumber() {
        return jpa.findMaxVersionNumber();
    }

    @Override
    public PageResult<AiAgentConfigurationVersion> findPaged(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "versionNumber"));
        Page<AiAgentConfigurationVersionEntity> result = jpa.findAll(pageRequest);
        List<AiAgentConfigurationVersion> content = result.getContent().stream()
                .map(AiAgentConfigurationVersionPersistenceMapper::toDomain)
                .toList();
        return new PageResult<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
