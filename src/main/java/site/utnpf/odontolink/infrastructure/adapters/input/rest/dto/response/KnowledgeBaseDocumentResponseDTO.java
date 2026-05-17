package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentKind;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;

import java.time.Instant;

/**
 * Vista de respuesta para un documento de la Knowledge Base (RF33).
 *
 * <p>{@code inlineContent} se incluye solo para documentos FAQ (no se
 * persiste para archivos binarios; en archivos viaja null y queda omitido
 * gracias a {@link JsonInclude}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeBaseDocumentResponseDTO {

    private Long id;
    private String title;
    private KnowledgeBaseDocumentKind kind;
    private String inlineContent;
    private String originalFileName;
    private long sizeBytes;
    private String contentType;
    private String providerDataSourceId;
    private KnowledgeBaseDocumentStatus status;
    private String lastIndexingJobId;
    private Instant lastIndexedAt;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

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
