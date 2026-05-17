package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;

/**
 * Documento de la Knowledge Base administrada (RF33).
 *
 * <p>Agregado que vive en BD como espejo de un data source del proveedor de
 * IA. La fuente de verdad del *contenido* es el bucket Spaces + el indice
 * remoto; nosotros mantenemos los metadatos (titulo, kind, estado del
 * pipeline) para listar rapido, auditar quien subio que y recuperar el
 * {@code storedObjectKey} para borrar el binario cuando se elimina el
 * documento.
 *
 * <p>El ciclo de vida lo expresan los metodos {@code mark*} y avanza el
 * {@link KnowledgeBaseDocumentStatus} segun el siguiente flujo feliz:
 * <pre>
 *   PENDING_UPLOAD -> UPLOADED -> REGISTERED -> INDEXING -> INDEXED
 * </pre>
 * Cualquier estado puede saltar a {@link KnowledgeBaseDocumentStatus#FAILED}
 * con un {@code errorMessage} que el administrador ve y usa para decidir si
 * reintentar o eliminar.
 */
public class KnowledgeBaseDocument {

    private Long id;
    private String title;
    private KnowledgeBaseDocumentKind kind;
    /**
     * Texto inline solo aplicable a {@link KnowledgeBaseDocumentKind#FAQ_TEXT}.
     * Se conserva en BD para que el administrador pueda re-editar la FAQ sin
     * tener que descargar el TXT desde el bucket.
     */
    private String inlineContent;
    private String originalFileName;
    /**
     * Clave del objeto dentro del bucket Spaces. Sirve a dos propositos:
     * <ol>
     *   <li>borrar el binario cuando se elimina el documento,</li>
     *   <li>componer el {@code item_path} del data source en el proveedor.</li>
     * </ol>
     */
    private String storedObjectKey;
    private long sizeBytes;
    private String contentType;
    /**
     * UUID del data source asociado en el proveedor. Null mientras el upload
     * fue exitoso pero el registro todavia no se confirmo (estado
     * {@link KnowledgeBaseDocumentStatus#UPLOADED}).
     */
    private String providerDataSourceId;
    private KnowledgeBaseDocumentStatus status;
    private String lastIndexingJobId;
    private Instant lastIndexedAt;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public KnowledgeBaseDocument() {
    }

    public KnowledgeBaseDocument(Long id,
                                 String title,
                                 KnowledgeBaseDocumentKind kind,
                                 String inlineContent,
                                 String originalFileName,
                                 String storedObjectKey,
                                 long sizeBytes,
                                 String contentType,
                                 String providerDataSourceId,
                                 KnowledgeBaseDocumentStatus status,
                                 String lastIndexingJobId,
                                 Instant lastIndexedAt,
                                 String errorMessage,
                                 Instant createdAt,
                                 Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.kind = kind;
        this.inlineContent = inlineContent;
        this.originalFileName = originalFileName;
        this.storedObjectKey = storedObjectKey;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.providerDataSourceId = providerDataSourceId;
        this.status = status;
        this.lastIndexingJobId = lastIndexingJobId;
        this.lastIndexedAt = lastIndexedAt;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Construye un documento FAQ a partir de texto plano. El service lo
     * envolvera en un TXT al subirlo al bucket; aqui simplemente registramos
     * el contenido inline para auditoria/edicion.
     */
    public static KnowledgeBaseDocument faq(String title, String content) {
        validateNonBlank(title, "title");
        validateNonBlank(content, "content");
        Instant now = Instant.now();
        return new KnowledgeBaseDocument(
                null,
                title.trim(),
                KnowledgeBaseDocumentKind.FAQ_TEXT,
                content,
                title.trim().replaceAll("\\s+", "_") + ".txt",
                null,
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                "text/plain",
                null,
                KnowledgeBaseDocumentStatus.PENDING_UPLOAD,
                null,
                null,
                null,
                now,
                now
        );
    }

    /**
     * Construye un documento basado en un archivo subido por el administrador.
     */
    public static KnowledgeBaseDocument file(String title,
                                             String originalFileName,
                                             long sizeBytes,
                                             String contentType) {
        validateNonBlank(title, "title");
        validateNonBlank(originalFileName, "originalFileName");
        if (sizeBytes <= 0) {
            throw new InvalidBusinessRuleException("El archivo no puede estar vacio.");
        }
        Instant now = Instant.now();
        return new KnowledgeBaseDocument(
                null,
                title.trim(),
                KnowledgeBaseDocumentKind.UPLOADED_FILE,
                null,
                originalFileName,
                null,
                sizeBytes,
                contentType,
                null,
                KnowledgeBaseDocumentStatus.PENDING_UPLOAD,
                null,
                null,
                null,
                now,
                now
        );
    }

    public void markUploaded(String storedObjectKey) {
        if (storedObjectKey == null || storedObjectKey.isBlank()) {
            throw new InvalidBusinessRuleException("storedObjectKey es obligatorio para marcar UPLOADED.");
        }
        this.storedObjectKey = storedObjectKey;
        this.status = KnowledgeBaseDocumentStatus.UPLOADED;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markRegistered(String providerDataSourceId) {
        if (providerDataSourceId == null || providerDataSourceId.isBlank()) {
            throw new InvalidBusinessRuleException("providerDataSourceId es obligatorio para marcar REGISTERED.");
        }
        this.providerDataSourceId = providerDataSourceId;
        this.status = KnowledgeBaseDocumentStatus.REGISTERED;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markIndexing(String indexingJobId) {
        this.lastIndexingJobId = indexingJobId;
        this.status = KnowledgeBaseDocumentStatus.INDEXING;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markIndexed() {
        this.status = KnowledgeBaseDocumentStatus.INDEXED;
        this.lastIndexedAt = Instant.now();
        this.errorMessage = null;
        this.updatedAt = this.lastIndexedAt;
    }

    public void markFailed(String reason) {
        this.status = KnowledgeBaseDocumentStatus.FAILED;
        this.errorMessage = reason;
        this.updatedAt = Instant.now();
    }

    private static void validateNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidBusinessRuleException("El campo '" + field + "' es obligatorio.");
        }
    }

    // Getters / setters --------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public KnowledgeBaseDocumentKind getKind() {
        return kind;
    }

    public String getInlineContent() {
        return inlineContent;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredObjectKey() {
        return storedObjectKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getProviderDataSourceId() {
        return providerDataSourceId;
    }

    public KnowledgeBaseDocumentStatus getStatus() {
        return status;
    }

    public String getLastIndexingJobId() {
        return lastIndexingJobId;
    }

    public Instant getLastIndexedAt() {
        return lastIndexedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
