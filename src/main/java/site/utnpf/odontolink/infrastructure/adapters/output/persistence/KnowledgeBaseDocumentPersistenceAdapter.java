package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;
import site.utnpf.odontolink.domain.repository.KnowledgeBaseDocumentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.KnowledgeBaseDocumentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaKnowledgeBaseDocumentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.KnowledgeBaseDocumentPersistenceMapper;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia para {@link KnowledgeBaseDocument} (RF33).
 *
 * <p>Politica transaccional uniforme con el resto de adapters: clase
 * {@code @Transactional(readOnly = true)} y las escrituras anotadas explicitamente.
 */
@Component
@Transactional(readOnly = true)
public class KnowledgeBaseDocumentPersistenceAdapter implements KnowledgeBaseDocumentRepository {

    private final JpaKnowledgeBaseDocumentRepository jpaRepository;

    public KnowledgeBaseDocumentPersistenceAdapter(JpaKnowledgeBaseDocumentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<KnowledgeBaseDocument> findAllOrderByCreatedAtDesc() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(KnowledgeBaseDocumentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<KnowledgeBaseDocument> findById(Long id) {
        return jpaRepository.findById(id).map(KnowledgeBaseDocumentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<KnowledgeBaseDocument> findByProviderDataSourceId(String providerDataSourceId) {
        return jpaRepository.findByProviderDataSourceId(providerDataSourceId)
                .map(KnowledgeBaseDocumentPersistenceMapper::toDomain);
    }

    @Override
    public List<KnowledgeBaseDocument> findByStatusIn(List<KnowledgeBaseDocumentStatus> statuses) {
        return jpaRepository.findByStatusIn(statuses).stream()
                .map(KnowledgeBaseDocumentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public KnowledgeBaseDocument save(KnowledgeBaseDocument document) {
        KnowledgeBaseDocumentEntity entity = KnowledgeBaseDocumentPersistenceMapper.toEntity(document);
        KnowledgeBaseDocumentEntity saved = jpaRepository.save(entity);
        return KnowledgeBaseDocumentPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
