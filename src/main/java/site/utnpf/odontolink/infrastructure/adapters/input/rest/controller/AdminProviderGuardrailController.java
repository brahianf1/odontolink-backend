package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IProviderGuardrailAdminUseCase;
import site.utnpf.odontolink.domain.model.ProviderGuardrail;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ProviderGuardrailAttachmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ProviderGuardrailResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ProviderGuardrailRestMapper;

import java.util.List;

/**
 * Gestion de guardrails nativos del proveedor (DigitalOcean Gradient hoy)
 * (RF31).
 *
 * <p>Distinto del CRUD de {@code /api/admin/ai-agent/policy-rules}: aqui no
 * se crean ni se borran recursos (la API del proveedor no lo permite). El
 * admin solo puede:
 * <ul>
 *   <li>Listar el espejo local.</li>
 *   <li>Refrescar el espejo consultando al proveedor (descubre nuevos).</li>
 *   <li>Cambiar la intencion de attach/priority de cada guardrail. El
 *       cambio se propaga al proveedor en el proximo {@code publish()}.</li>
 * </ul>
 *
 * <p>La configuracion fina de los guardrails (categorias del Sensitive Data
 * Detection, default_response, etc.) NO se expone via API publica de DO; el
 * admin debe editarla manualmente en el dashboard del proveedor.
 */
@RestController
@RequestMapping("/api/admin/ai-agent/provider-guardrails")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Guardrails nativos del proveedor (filtros de plataforma) (RF31).")
public class AdminProviderGuardrailController {

    private final IProviderGuardrailAdminUseCase useCase;

    public AdminProviderGuardrailController(IProviderGuardrailAdminUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Listar guardrails del proveedor (espejo local)")
    @GetMapping
    public ResponseEntity<List<ProviderGuardrailResponseDTO>> list() {
        List<ProviderGuardrail> all = useCase.listGuardrails();
        return ResponseEntity.ok(ProviderGuardrailRestMapper.toResponseList(all));
    }

    @Operation(summary = "Refrescar desde el proveedor",
            description = "Consulta al proveedor y sincroniza el espejo local. " +
                    "Agrega los guardrails nuevos, actualiza metadata descriptiva " +
                    "de los existentes. Preserva la intencion de attach del admin " +
                    "para los que ya estaban localmente.")
    @PostMapping("/refresh")
    public ResponseEntity<List<ProviderGuardrailResponseDTO>> refresh() {
        List<ProviderGuardrail> all = useCase.refreshFromProvider();
        return ResponseEntity.ok(ProviderGuardrailRestMapper.toResponseList(all));
    }

    @Operation(summary = "Actualizar attach + priority de un guardrail",
            description = "El cambio se propaga al proveedor en el proximo POST /publish " +
                    "del agente. Cualquier cambio revierte la config a DRAFT.")
    @PutMapping("/{id}/attachment")
    public ResponseEntity<ProviderGuardrailResponseDTO> updateAttachment(
            @PathVariable Long id,
            @Valid @RequestBody ProviderGuardrailAttachmentRequestDTO request) {
        ProviderGuardrail saved = useCase.updateAttachment(id, request.isAttached(), request.getPriority());
        return ResponseEntity.ok(ProviderGuardrailRestMapper.toResponse(saved));
    }
}
