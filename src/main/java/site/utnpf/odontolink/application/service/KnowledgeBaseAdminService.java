package site.utnpf.odontolink.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IKnowledgeBaseAdminUseCase;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort.IndexingJobSnapshot;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort.RegisteredDataSource;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.application.port.out.StorageException;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentKind;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.repository.KnowledgeBaseDocumentRepository;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de aplicacion para la administracion de la Knowledge Base (RF33).
 *
 * <p>Pipeline del alta de un documento:
 * <ol>
 *   <li>Validacion (tamanio, MIME type).</li>
 *   <li>Persistir el documento en estado {@code PENDING_UPLOAD}.</li>
 *   <li>Subir el binario al bucket Spaces dedicado a la KB.</li>
 *   <li>Marcar {@code UPLOADED} con la {@code storedObjectKey}.</li>
 *   <li>Registrar el data source en el proveedor de IA.</li>
 *   <li>Marcar {@code REGISTERED}.</li>
 *   <li>Disparar el indexing job y guardar el {@code lastIndexingJobId}.</li>
 *   <li>Marcar {@code INDEXING}.</li>
 * </ol>
 *
 * <p>Si cualquier paso intermedio falla, el documento queda en {@code FAILED}
 * con {@code errorMessage} descriptivo y la transaccion local se commitea
 * igual: el administrador puede inspeccionar el estado y decidir reintentar
 * o borrar. Esto evita que un blip del proveedor deje un objeto huerfano
 * sin ningun registro en BD.
 *
 * <p>El pipeline es sincronico para mantener el codigo simple. Si en el
 * futuro los documentos crecen (mucho indexing tiempo) se puede mover a
 * background con {@code @Async}.
 */
@Transactional
public class KnowledgeBaseAdminService implements IKnowledgeBaseAdminUseCase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAdminService.class);

    /**
     * MIME types aceptados por el endpoint de upload. Coincide con los
     * formatos que la KB de DigitalOcean Gradient indexa correctamente.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/json",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final KnowledgeBaseDocumentRepository documentRepository;
    private final IKnowledgeBaseProviderPort kbProvider;
    private final IObjectStoragePort aiKbStorage;
    private final String knowledgeBaseUuid;
    private final String bucketName;
    private final String bucketRegion;
    private final String keyPrefix;
    private final long maxUploadBytes;

    public KnowledgeBaseAdminService(KnowledgeBaseDocumentRepository documentRepository,
                                     IKnowledgeBaseProviderPort kbProvider,
                                     IObjectStoragePort aiKbStorage,
                                     String knowledgeBaseUuid,
                                     String bucketName,
                                     String bucketRegion,
                                     String keyPrefix,
                                     long maxUploadBytes) {
        this.documentRepository = documentRepository;
        this.kbProvider = kbProvider;
        this.aiKbStorage = aiKbStorage;
        this.knowledgeBaseUuid = knowledgeBaseUuid;
        this.bucketName = bucketName;
        this.bucketRegion = bucketRegion;
        this.keyPrefix = keyPrefix;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocument> listDocuments() {
        return documentRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseDocument getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBaseDocument", "id", String.valueOf(id)));
    }

    @Override
    public KnowledgeBaseDocument addFaqDocument(String title, String content) {
        validateModuleConfigured();
        if (content == null || content.isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El contenido de la FAQ no puede estar vacio.",
                    AiAgentErrorCodes.AI_KB_FILE_EMPTY);
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxUploadBytes) {
            throw new InvalidBusinessRuleException(
                    "El contenido excede el tamanio maximo permitido (" + maxUploadBytes + " bytes).",
                    AiAgentErrorCodes.AI_KB_FILE_TOO_LARGE);
        }

        KnowledgeBaseDocument doc = KnowledgeBaseDocument.faq(title, content);
        return persistAndPropagate(doc, bytes, doc.getOriginalFileName(), "text/plain");
    }

    @Override
    public KnowledgeBaseDocument addFileDocument(String title,
                                                 String originalFileName,
                                                 byte[] content,
                                                 String contentType) {
        validateModuleConfigured();
        if (content == null || content.length == 0) {
            throw new InvalidBusinessRuleException(
                    "El archivo es obligatorio y no puede estar vacio.",
                    AiAgentErrorCodes.AI_KB_FILE_EMPTY);
        }
        if (content.length > maxUploadBytes) {
            throw new InvalidBusinessRuleException(
                    "El archivo excede el tamanio maximo permitido (" + maxUploadBytes + " bytes).",
                    AiAgentErrorCodes.AI_KB_FILE_TOO_LARGE);
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidBusinessRuleException(
                    "Tipo de archivo no soportado. Tipos aceptados: " + ALLOWED_CONTENT_TYPES,
                    AiAgentErrorCodes.AI_KB_UNSUPPORTED_TYPE);
        }

        KnowledgeBaseDocument doc = KnowledgeBaseDocument.file(title, originalFileName, content.length, contentType);
        return persistAndPropagate(doc, content, originalFileName, contentType);
    }

    /**
     * Recibe un documento recien construido por la factory y los bytes
     * efectivos a subir, y ejecuta el pipeline completo. La separacion
     * con los entry points permite reusar el mismo camino para FAQ y file.
     */
    private KnowledgeBaseDocument persistAndPropagate(KnowledgeBaseDocument doc,
                                                     byte[] bytes,
                                                     String fileNameForKey,
                                                     String contentType) {
        // 1) Persistimos el documento PENDING_UPLOAD primero. Asi si el
        // siguiente paso falla por timeout, hay traza local.
        KnowledgeBaseDocument persisted = documentRepository.save(doc);

        // 2) Subimos el binario al bucket. La key incluye un UUID para
        // evitar colisiones y permite reintentos sin pisar el objeto previo.
        String storedKey = buildStoredKey(fileNameForKey);
        try {
            aiKbStorage.upload(storedKey, bytes, contentType);
            persisted.markUploaded(storedKey);
            persisted = documentRepository.save(persisted);
        } catch (StorageException ex) {
            persisted.markFailed("Falla al subir el binario al storage: " + ex.getMessage());
            documentRepository.save(persisted);
            throw ex;
        }

        // 3) Registramos el data source en el proveedor. Si falla, el
        // documento queda UPLOADED + FAILED; el binario queda en nuestro
        // bucket y puede limpiarse via deleteDocument.
        RegisteredDataSource registered;
        try {
            registered = kbProvider.registerSpacesDataSource(
                    knowledgeBaseUuid, bucketName, bucketRegion, storedKey);
            persisted.markRegistered(registered.providerDataSourceId());
            persisted = documentRepository.save(persisted);
        } catch (LlmProviderException ex) {
            persisted.markFailed("Falla al registrar el data source en el proveedor: " + ex.getMessage());
            documentRepository.save(persisted);
            throw ex;
        }

        // 4) Disparamos la indexacion. Si falla, devolvemos el documento en
        // REGISTERED + errorMessage: el binario esta listo, la indexacion se
        // puede reintentar via triggerReindex.
        try {
            IndexingJobSnapshot job = kbProvider.startIndexing(
                    knowledgeBaseUuid, List.of(registered.providerDataSourceId()));
            persisted.markIndexing(job.jobId());
            return documentRepository.save(persisted);
        } catch (LlmProviderException ex) {
            log.warn("Falla al disparar la indexacion del documento id={}: {}", persisted.getId(), ex.getMessage());
            persisted.markFailed("Falla al iniciar la indexacion: " + ex.getMessage());
            return documentRepository.save(persisted);
        }
    }

    @Override
    public KnowledgeBaseDocument updateDocument(Long id, String title, String content) {
        validateModuleConfigured();
        KnowledgeBaseDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KnowledgeBaseDocument", "id", String.valueOf(id)));

        // Solo aceptamos contenido para FAQs. Para archivos subidos rechazamos
        // con codigo estable: la edicion de binarios pasa por delete + create.
        if (content != null && doc.getKind() != KnowledgeBaseDocumentKind.FAQ_TEXT) {
            throw new InvalidBusinessRuleException(
                    "Los documentos de tipo UPLOADED_FILE no admiten edicion de contenido. " +
                            "Elimine y vuelva a subir el archivo.",
                    AiAgentErrorCodes.AI_KB_UNSUPPORTED_TYPE);
        }

        // Rename siempre que llegue distinto. renameTo() valida no-vacio + tamanio.
        if (title != null && !title.equals(doc.getTitle())) {
            doc.renameTo(title);
        }

        // Si es FAQ y el contenido cambia, re-subimos el TXT al mismo
        // storedObjectKey (reemplazo, sin generar uno nuevo) y disparamos
        // reindex del data source asociado. Asi el indice remoto refleja
        // la nueva FAQ sin proliferar objetos huerfanos en el bucket.
        boolean contentChanged = content != null
                && doc.getKind() == KnowledgeBaseDocumentKind.FAQ_TEXT
                && !content.equals(doc.getInlineContent());
        if (contentChanged) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > maxUploadBytes) {
                throw new InvalidBusinessRuleException(
                        "El contenido excede el tamanio maximo permitido (" + maxUploadBytes + " bytes).",
                        AiAgentErrorCodes.AI_KB_FILE_TOO_LARGE);
            }
            doc.updateFaqContent(content);
            // Re-upload al mismo key: pisa el objeto previo (S3 PUT es atomico).
            aiKbStorage.upload(doc.getStoredObjectKey(), bytes, "text/plain");
        }

        KnowledgeBaseDocument saved = documentRepository.save(doc);

        if (contentChanged && saved.getProviderDataSourceId() != null) {
            try {
                IndexingJobSnapshot job = kbProvider.startIndexing(
                        knowledgeBaseUuid, List.of(saved.getProviderDataSourceId()));
                saved.markIndexing(job.jobId());
                saved = documentRepository.save(saved);
            } catch (LlmProviderException ex) {
                log.warn("Falla al disparar reindex tras editar FAQ id={}: {}", id, ex.getMessage());
                saved.markFailed("Falla al re-indexar la FAQ tras edicion: " + ex.getMessage());
                saved = documentRepository.save(saved);
            }
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(Long id) {
        validateModuleConfigured();
        KnowledgeBaseDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KnowledgeBaseDocument", "id", String.valueOf(id)));

        // Para FAQs servimos el contenido inline directamente: evita el
        // round-trip al bucket y siempre devuelve el texto fuente (lo que
        // el admin tipeo), no su serializacion intermedia a TXT.
        if (doc.getKind() == KnowledgeBaseDocumentKind.FAQ_TEXT) {
            byte[] bytes = doc.getInlineContent() == null
                    ? new byte[0]
                    : doc.getInlineContent().getBytes(StandardCharsets.UTF_8);
            return new DocumentDownload(bytes, "text/plain; charset=utf-8", doc.getOriginalFileName());
        }

        if (doc.getStoredObjectKey() == null || doc.getStoredObjectKey().isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El documento todavia no fue subido al bucket; no hay binario que descargar.",
                    AiAgentErrorCodes.AI_KB_FILE_EMPTY);
        }

        IObjectStoragePort.DownloadedObject downloaded = aiKbStorage.download(doc.getStoredObjectKey());
        // Preferimos el contentType persistido en BD (el que el admin subio)
        // antes que el que reporta el storage, porque algunos backends S3-compat
        // devuelven application/octet-stream cuando el upload no envio header.
        String contentType = doc.getContentType() != null && !doc.getContentType().isBlank()
                ? doc.getContentType()
                : downloaded.contentType();
        return new DocumentDownload(downloaded.content(), contentType, doc.getOriginalFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public IndexingJobSnapshot getIndexingJob(String jobId) {
        validateModuleConfigured();
        if (jobId == null || jobId.isBlank()) {
            throw new InvalidBusinessRuleException("jobId es obligatorio.");
        }
        return kbProvider.getIndexingJobStatus(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<KnowledgeBaseDocument> listDocumentsPaged(
            site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus status,
            int page,
            int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return documentRepository.findPaged(status, safePage, safeSize);
    }

    @Override
    public void deleteDocument(Long id) {
        validateModuleConfigured();
        KnowledgeBaseDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBaseDocument", "id", String.valueOf(id)));

        // Borrado idempotente en cascada. Si alguno de los lados ya no tiene
        // el recurso, registramos warning y seguimos: el objetivo es dejar
        // el sistema en estado limpio aunque haya inconsistencias previas.
        if (doc.getProviderDataSourceId() != null) {
            try {
                kbProvider.deleteDataSource(knowledgeBaseUuid, doc.getProviderDataSourceId());
            } catch (LlmProviderException ex) {
                log.warn("Falla al borrar el data source remoto id={} del documento {}: {}",
                        doc.getProviderDataSourceId(), id, ex.getMessage());
            }
        }
        if (doc.getStoredObjectKey() != null) {
            try {
                aiKbStorage.delete(doc.getStoredObjectKey());
            } catch (StorageException ex) {
                log.warn("Falla al borrar el binario {} del documento {}: {}",
                        doc.getStoredObjectKey(), id, ex.getMessage());
            }
        }
        documentRepository.deleteById(id);

        // Reindexamos para sacar el documento eliminado del indice. No es
        // critico (el data source ya esta borrado en el proveedor), pero
        // mantiene consistencia.
        try {
            kbProvider.startIndexing(knowledgeBaseUuid, Collections.emptyList());
        } catch (LlmProviderException ex) {
            log.warn("Falla al disparar reindex tras borrar documento {}: {}", id, ex.getMessage());
        }
    }

    @Override
    public IndexingJobSnapshot triggerReindex() {
        validateModuleConfigured();
        return kbProvider.startIndexing(knowledgeBaseUuid, Collections.emptyList());
    }

    @Override
    public KnowledgeBaseDocument refreshIndexingStatus(Long id) {
        validateModuleConfigured();
        KnowledgeBaseDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBaseDocument", "id", String.valueOf(id)));

        if (doc.getLastIndexingJobId() == null) {
            // No hay job que consultar; devolvemos el estado tal cual.
            return doc;
        }

        IndexingJobSnapshot snapshot = kbProvider.getIndexingJobStatus(doc.getLastIndexingJobId());
        // El set de status de DO incluye INDEX_JOB_STATUS_COMPLETED, *_RUNNING,
        // *_FAILED, *_PENDING. Reducimos a INDEXED/INDEXING/FAILED para
        // mantener el dominio simple.
        String status = snapshot.status() == null ? "" : snapshot.status();
        if (status.contains("COMPLETED") || status.contains("SUCCESS")) {
            doc.markIndexed();
        } else if (status.contains("FAILED") || status.contains("ERROR")) {
            doc.markFailed(snapshot.errorMessage() == null ? "Indexing job fallo en el proveedor." : snapshot.errorMessage());
        } else {
            // Sigue corriendo: mantenemos INDEXING. Solo refrescamos updatedAt
            // via markIndexing con el mismo jobId.
            doc.markIndexing(doc.getLastIndexingJobId());
        }
        return documentRepository.save(doc);
    }

    private String buildStoredKey(String fileName) {
        String safeName = fileName == null ? "document" : fileName;
        // El prefijo separa los binarios de la KB del resto del bucket; el
        // UUID asegura unicidad incluso si el admin sube dos archivos con
        // el mismo nombre. Mantenemos el nombre original al final para que
        // el listado del bucket sea legible humanamente.
        return (keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/")
                + UUID.randomUUID() + "/" + safeName;
    }

    private void validateModuleConfigured() {
        if (knowledgeBaseUuid == null || knowledgeBaseUuid.isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El modulo de Knowledge Base no esta configurado: falta DIGITALOCEAN_KNOWLEDGE_BASE_UUID.");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El modulo de Knowledge Base no esta configurado: falta AI_KB_STORAGE_S3_BUCKET.");
        }
        if (bucketRegion == null || bucketRegion.isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El modulo de Knowledge Base no esta configurado: falta AI_KB_STORAGE_S3_REGION.");
        }
    }
}
