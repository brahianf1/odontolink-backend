package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.KnowledgeBaseDocumentEntity;

/**
 * Mapper estatico entre {@link KnowledgeBaseDocument} (dominio) y
 * {@link KnowledgeBaseDocumentEntity} (persistencia).
 */
public final class KnowledgeBaseDocumentPersistenceMapper {

    private KnowledgeBaseDocumentPersistenceMapper() {
    }

    public static KnowledgeBaseDocument toDomain(KnowledgeBaseDocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new KnowledgeBaseDocument(
                entity.getId(),
                entity.getTitle(),
                entity.getKind(),
                entity.getInlineContent(),
                entity.getOriginalFileName(),
                entity.getStoredObjectKey(),
                entity.getSizeBytes(),
                entity.getContentType(),
                entity.getProviderDataSourceId(),
                entity.getStatus(),
                entity.getLastIndexingJobId(),
                entity.getLastIndexedAt(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static KnowledgeBaseDocumentEntity toEntity(KnowledgeBaseDocument domain) {
        if (domain == null) {
            return null;
        }
        KnowledgeBaseDocumentEntity entity = new KnowledgeBaseDocumentEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setKind(domain.getKind());
        entity.setInlineContent(domain.getInlineContent());
        entity.setOriginalFileName(domain.getOriginalFileName());
        entity.setStoredObjectKey(domain.getStoredObjectKey());
        entity.setSizeBytes(domain.getSizeBytes());
        entity.setContentType(domain.getContentType());
        entity.setProviderDataSourceId(domain.getProviderDataSourceId());
        entity.setStatus(domain.getStatus());
        entity.setLastIndexingJobId(domain.getLastIndexingJobId());
        entity.setLastIndexedAt(domain.getLastIndexedAt());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
