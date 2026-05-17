package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.KnowledgeBaseDocumentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para {@link KnowledgeBaseDocumentEntity}.
 */
@Repository
public interface JpaKnowledgeBaseDocumentRepository extends JpaRepository<KnowledgeBaseDocumentEntity, Long> {

    List<KnowledgeBaseDocumentEntity> findAllByOrderByCreatedAtDesc();

    Optional<KnowledgeBaseDocumentEntity> findByProviderDataSourceId(String providerDataSourceId);

    List<KnowledgeBaseDocumentEntity> findByStatusIn(List<KnowledgeBaseDocumentStatus> statuses);

    Page<KnowledgeBaseDocumentEntity> findByStatus(KnowledgeBaseDocumentStatus status, Pageable pageable);
}
