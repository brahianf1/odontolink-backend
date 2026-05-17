package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.utnpf.odontolink.application.port.in.IKnowledgeBaseAdminUseCase;
import site.utnpf.odontolink.application.port.in.IKnowledgeBaseAdminUseCase.DocumentDownload;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort.IndexingJobSnapshot;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AddFaqDocumentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateKnowledgeBaseDocumentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.IndexingJobStatusResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.KnowledgeBaseDocumentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.KnowledgeBaseDocumentRestMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Adaptador de entrada REST para la administracion de la Knowledge Base
 * (RF33). La logica de validacion / subida / registro / indexacion vive en
 * {@code KnowledgeBaseAdminService}.
 */
@RestController
@RequestMapping("/api/admin/ai-agent/knowledge-base")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Gestion de la Knowledge Base (FAQs y archivos) que el agente usa via RAG (RF33)")
public class AdminKnowledgeBaseController {

    private final IKnowledgeBaseAdminUseCase useCase;

    public AdminKnowledgeBaseController(IKnowledgeBaseAdminUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Listar documentos de la Knowledge Base (paginado)",
            description = "Devuelve una pagina de documentos ordenados por createdAt descendente. " +
                    "Acepta filtro opcional por status (p. ej. FAILED, INDEXING) para que el FE " +
                    "muestre pestanias. Defaults: page=0, size=20. Maximo size=100.")
    @GetMapping("/documents")
    public ResponseEntity<PageResponseDTO<KnowledgeBaseDocumentResponseDTO>> listDocuments(
            @RequestParam(name = "status", required = false) KnowledgeBaseDocumentStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult<KnowledgeBaseDocument> docs = useCase.listDocumentsPaged(status, page, size);
        return ResponseEntity.ok(PageResponseDTO.of(docs, KnowledgeBaseDocumentRestMapper::toResponse));
    }

    @Operation(summary = "Obtener detalle de un documento")
    @GetMapping("/documents/{id}")
    public ResponseEntity<KnowledgeBaseDocumentResponseDTO> getDocument(@PathVariable Long id) {
        KnowledgeBaseDocument doc = useCase.getDocument(id);
        return ResponseEntity.ok(KnowledgeBaseDocumentRestMapper.toResponse(doc));
    }

    @Operation(summary = "Agregar FAQ a la Knowledge Base",
            description = "Acepta titulo + contenido en texto plano. El backend lo envuelve como TXT, " +
                    "lo sube al bucket Spaces, lo registra como data source y dispara la indexacion.")
    @PostMapping("/documents/faq")
    public ResponseEntity<KnowledgeBaseDocumentResponseDTO> addFaqDocument(
            @Valid @RequestBody AddFaqDocumentRequestDTO request) {
        KnowledgeBaseDocument doc = useCase.addFaqDocument(request.getTitle(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(KnowledgeBaseDocumentRestMapper.toResponse(doc));
    }

    @Operation(summary = "Agregar archivo a la Knowledge Base",
            description = "Multipart con campos 'title' (texto, max 200 chars) y 'file' (binario, max 10 MB). " +
                    "Formatos aceptados: PDF, TXT, MD, JSON, CSV, DOCX.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Multipart con titulo + archivo",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MultipartFile.class))))
    @PostMapping(value = "/documents/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KnowledgeBaseDocumentResponseDTO> addFileDocument(
            @RequestParam("title") @NotBlank @Size(max = 200) String title,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidBusinessRuleException(
                    "El archivo es obligatorio en el campo 'file'.",
                    AiAgentErrorCodes.AI_KB_FILE_EMPTY);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidBusinessRuleException(
                    "No se pudo leer el archivo subido: " + ex.getMessage());
        }
        KnowledgeBaseDocument doc = useCase.addFileDocument(
                title, file.getOriginalFilename(), bytes, file.getContentType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(KnowledgeBaseDocumentRestMapper.toResponse(doc));
    }

    @Operation(summary = "Editar documento (titulo y, en FAQs, contenido)",
            description = "Acepta cambio de titulo siempre. Para documentos FAQ_TEXT acepta tambien " +
                    "un nuevo content; si llega distinto al actual, el backend re-sube el TXT al bucket " +
                    "(mismo storedObjectKey) y dispara reindex del data source. Para archivos subidos " +
                    "el content debe llegar nulo: editar binarios pasa por delete + create.")
    @PutMapping("/documents/{id}")
    public ResponseEntity<KnowledgeBaseDocumentResponseDTO> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeBaseDocumentRequestDTO request) {
        KnowledgeBaseDocument updated = useCase.updateDocument(id, request.getTitle(), request.getContent());
        return ResponseEntity.ok(KnowledgeBaseDocumentRestMapper.toResponse(updated));
    }

    @Operation(summary = "Descargar el binario original del documento",
            description = "Para FAQs devuelve el inlineContent como text/plain UTF-8. Para archivos " +
                    "subidos lee el objeto desde el bucket Spaces y lo devuelve con el contentType " +
                    "original (PDF, DOCX, etc.). Util para que el admin verifique que se indexo.")
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        DocumentDownload download = useCase.downloadDocument(id);
        // El nombre original puede traer caracteres no-ASCII; lo codificamos como
        // RFC 5987 (filename*=UTF-8''...) para que navegadores y clientes
        // modernos lo decodifiquen correctamente.
        String safeName = URLEncoder.encode(
                download.fileName() == null ? "document" : download.fileName(),
                StandardCharsets.UTF_8);
        String disposition = "attachment; filename*=UTF-8''" + safeName;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(download.content().length)
                .body(download.content());
    }

    @Operation(summary = "Borrar documento de la Knowledge Base",
            description = "Borra en cascada: data source en el proveedor, binario del bucket y row local. " +
                    "Idempotente: si alguno ya no existe, sigue adelante.")
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        useCase.deleteDocument(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Refrescar estado de indexacion de un documento",
            description = "Pega al proveedor con el lastIndexingJobId del documento y actualiza el " +
                    "status local. Recomendacion: pollear cada 5-10s SOLO para documentos en estado " +
                    "INDEXING; parar al llegar a INDEXED/FAILED.")
    @PostMapping("/documents/{id}/refresh-status")
    public ResponseEntity<KnowledgeBaseDocumentResponseDTO> refreshStatus(@PathVariable Long id) {
        KnowledgeBaseDocument doc = useCase.refreshIndexingStatus(id);
        return ResponseEntity.ok(KnowledgeBaseDocumentRestMapper.toResponse(doc));
    }

    @Operation(summary = "Disparar reindexacion manual",
            description = "Fallback de troubleshooting cuando algun documento quedo en estado FAILED. " +
                    "Indexa todo el contenido pendiente de la KB en el proveedor.")
    @PostMapping("/reindex")
    public ResponseEntity<IndexingJobStatusResponseDTO> triggerReindex() {
        IndexingJobSnapshot snap = useCase.triggerReindex();
        return ResponseEntity.accepted().body(KnowledgeBaseDocumentRestMapper.toResponse(snap));
    }

    @Operation(summary = "Estado de un indexing job",
            description = "Consulta directa al proveedor por UUID del job (devuelto por POST /reindex o " +
                    "guardado en el lastIndexingJobId de un documento). No modifica documentos locales; " +
                    "es el endpoint preferido para pollear el progreso de un reindex global sin iterar.")
    @GetMapping("/indexing-jobs/{jobId}")
    public ResponseEntity<IndexingJobStatusResponseDTO> getIndexingJob(@PathVariable String jobId) {
        IndexingJobSnapshot snap = useCase.getIndexingJob(jobId);
        return ResponseEntity.ok(KnowledgeBaseDocumentRestMapper.toResponse(snap));
    }
}
