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
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAdminAuditEventResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentConfigurationVersionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AiAgentVersioningRestMapper;

import java.util.List;

/**
 * Versionado y audit log del agente IA (RF31). Permite al admin revisar
 * el historial de publishes y revertir a una version anterior.
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

    @Operation(summary = "Listar versiones publicadas",
            description = "Devuelve las versiones en orden descendente por numero. Cada publish " +
                    "exitoso (o rollback) crea una nueva entrada inmutable.")
    @GetMapping("/versions")
    public ResponseEntity<List<AiAgentConfigurationVersionResponseDTO>> listVersions() {
        List<AiAgentConfigurationVersion> versions = useCase.listVersions();
        return ResponseEntity.ok(AiAgentVersioningRestMapper.toVersionResponseList(versions));
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

    @Operation(summary = "Listar eventos de auditoria",
            description = "Devuelve los eventos de lifecycle del agente IA (publish, rollback, " +
                    "cambios de la policy de gobernanza) en orden descendente. Limit configurable, " +
                    "default 100, maximo 500.")
    @GetMapping("/audit-events")
    public ResponseEntity<List<AiAdminAuditEventResponseDTO>> listAuditEvents(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        List<AiAdminAuditEvent> events = useCase.listAuditEvents(limit);
        return ResponseEntity.ok(AiAgentVersioningRestMapper.toAuditResponseList(events));
    }
}
