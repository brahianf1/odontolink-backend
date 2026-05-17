package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IAiAgentVersioningUseCase;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAdminAuditEventResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentConfigurationVersionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AiAgentVersioningRestMapper;

import java.time.Instant;

/**
 * Versionado y audit log del agente IA (RF31). Permite al admin revisar
 * el historial de publishes, revertir a una version anterior y auditar
 * eventos de lifecycle / governance.
 */
@RestController
@RequestMapping("/api/admin/ai-agent")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Historial de versiones publicadas y audit log del agente IA (RF31)")
public class AdminAiAgentVersioningController {

    private final IAiAgentVersioningUseCase useCase;

    public AdminAiAgentVersioningController(IAiAgentVersioningUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Listar versiones publicadas (paginado)",
            description = "Devuelve una pagina de versiones en orden descendente por versionNumber. " +
                    "Cada publish exitoso o rollback crea una nueva entrada inmutable. Defaults: " +
                    "page=0, size=20. Maximo size=100.")
    @GetMapping("/versions")
    public ResponseEntity<PageResponseDTO<AiAgentConfigurationVersionResponseDTO>> listVersions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult<AiAgentConfigurationVersion> versions = useCase.listVersionsPaged(page, size);
        return ResponseEntity.ok(PageResponseDTO.of(versions, AiAgentVersioningRestMapper::toResponse));
    }

    @Operation(summary = "Rollback a una version anterior",
            description = "Re-aplica el contenido de la version indicada a la configuracion vigente y " +
                    "lo re-publica al proveedor. NO modifica versiones antiguas: genera una version " +
                    "nueva con numero correlativo, manteniendo el historial lineal.")
    @PostMapping("/versions/{versionNumber}/rollback")
    public ResponseEntity<AiAgentConfigurationVersionResponseDTO> rollback(@PathVariable int versionNumber) {
        AiAgentConfigurationVersion newVersion = useCase.rollbackToVersion(versionNumber);
        return ResponseEntity.ok(AiAgentVersioningRestMapper.toResponse(newVersion));
    }

    @Operation(summary = "Listar eventos de auditoria (paginado + filtros)",
            description = "Devuelve eventos de lifecycle (publish, publish-failed, rollback) y de " +
                    "governance (policy update) en orden descendente cronologico. Filtros opcionales: " +
                    "type (AGENT_PUBLISH | AGENT_PUBLISH_FAILED | AGENT_ROLLBACK | GOVERNANCE_POLICY_UPDATED), " +
                    "from / to (ISO-8601 timestamps; rango half-open). Defaults: page=0, size=50. " +
                    "Maximo size=200.")
    @GetMapping("/audit-events")
    public ResponseEntity<PageResponseDTO<AiAdminAuditEventResponseDTO>> listAuditEvents(
            @RequestParam(name = "type", required = false) AiAdminAuditEvent.Type type,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        PageResult<AiAdminAuditEvent> events = useCase.listAuditEventsPaged(type, from, to, page, size);
        return ResponseEntity.ok(PageResponseDTO.of(events, AiAgentVersioningRestMapper::toResponse));
    }
}
