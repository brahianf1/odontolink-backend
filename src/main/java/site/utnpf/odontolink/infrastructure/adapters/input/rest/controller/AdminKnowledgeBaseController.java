package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.utnpf.odontolink.application.port.in.IKnowledgeBaseAdminUseCase;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort.IndexingJobSnapshot;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AddFaqDocumentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.IndexingJobStatusResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.KnowledgeBaseDocumentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.KnowledgeBaseDocumentRestMapper;

import java.io.IOException;
import java.util.List;

/**
 * Adaptador de entrada REST para la administracion de la Knowledge Base
 * (RF33).
 *
 * <p>La logica de validacion / subida / registro / indexacion vive en
 * {@code KnowledgeBaseAdminService}. El controller se limita a:
 * <ul>
 *   <li>aplicar la autorizacion {@code ROLE_ADMIN},</li>
 *   <li>extraer bytes del {@link MultipartFile},</li>
 *   <li>mapear request <-> dominio <-> response.</li>
 * </ul>
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

    @Operation(summary = "Listar documentos de la Knowledge Base")
    @GetMapping("/documents")
    public ResponseEntity<List<KnowledgeBaseDocumentResponseDTO>> listDocuments() {
        List<KnowledgeBaseDocument> docs = useCase.listDocuments();
        return ResponseEntity.ok(KnowledgeBaseDocumentRestMapper.toResponseList(docs));
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
                    "El archivo es obligatorio en el campo 'file'.")
                    .withErrorCode(AiAgentErrorCodes.AI_KB_FILE_EMPTY);
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

    @Operation(summary = "Borrar documento de la Knowledge Base",
            description = "Borra en cascada: data source en el proveedor, binario del bucket y row local. " +
                    "Idempotente: si alguno ya no existe, sigue adelante.")
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        useCase.deleteDocument(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Refrescar estado de indexacion de un documento",
            description = "Pega al proveedor con el lastIndexingJobId y actualiza el status local.")
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
}
