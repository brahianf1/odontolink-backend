package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentKind;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_kb_documents} (RF33).
 *
 * <p>Indices:
 * <ul>
 *   <li>{@code provider_data_source_id}: unicidad. MySQL permite multiples
 *       NULL en columnas unicas, asi que el indice no impide multiples
 *       documentos en estado PENDING_UPLOAD/UPLOADED simultaneos.</li>
 *   <li>{@code status}: lookup frecuente por estado para refresh batch.</li>
 * </ul>
 */
@Entity
@Table(name = "ai_kb_documents", indexes = {
        @Index(name = "ux_ai_kb_documents_provider_ds", columnList = "provider_data_source_id", unique = true),
        @Index(name = "ix_ai_kb_documents_status", columnList = "status")
})
public class KnowledgeBaseDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private KnowledgeBaseDocumentKind kind;

    /**
     * Texto inline solo para FAQs. Permite que el admin re-edite el
     * contenido sin descargar el TXT desde el bucket.
     */
    @Column(name = "inline_content", columnDefinition = "TEXT")
    private String inlineContent;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "stored_object_key", length = 500)
    private String storedObjectKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "provider_data_source_id", length = 100)
    private String providerDataSourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KnowledgeBaseDocumentStatus status;

    @Column(name = "last_indexing_job_id", length = 100)
    private String lastIndexingJobId;

    @Column(name = "last_indexed_at")
    private Instant lastIndexedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public KnowledgeBaseDocumentEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public KnowledgeBaseDocumentKind getKind() {
        return kind;
    }

    public void setKind(KnowledgeBaseDocumentKind kind) {
        this.kind = kind;
    }

    public String getInlineContent() {
        return inlineContent;
    }

    public void setInlineContent(String inlineContent) {
        this.inlineContent = inlineContent;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredObjectKey() {
        return storedObjectKey;
    }

    public void setStoredObjectKey(String storedObjectKey) {
        this.storedObjectKey = storedObjectKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getProviderDataSourceId() {
        return providerDataSourceId;
    }

    public void setProviderDataSourceId(String providerDataSourceId) {
        this.providerDataSourceId = providerDataSourceId;
    }

    public KnowledgeBaseDocumentStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeBaseDocumentStatus status) {
        this.status = status;
    }

    public String getLastIndexingJobId() {
        return lastIndexingJobId;
    }

    public void setLastIndexingJobId(String lastIndexingJobId) {
        this.lastIndexingJobId = lastIndexingJobId;
    }

    public Instant getLastIndexedAt() {
        return lastIndexedAt;
    }

    public void setLastIndexedAt(Instant lastIndexedAt) {
        this.lastIndexedAt = lastIndexedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
