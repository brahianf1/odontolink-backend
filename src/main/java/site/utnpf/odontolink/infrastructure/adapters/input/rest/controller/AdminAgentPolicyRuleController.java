package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IAgentPolicyRuleAdminUseCase;
import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AgentPolicyRuleRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AgentPolicyRuleResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AgentPolicyRuleRestMapper;

import java.util.List;

/**
 * CRUD de reglas de comportamiento del agente IA (RF31, RF32). El admin
 * define todas las reglas desde el panel; el sistema NO provee textos por
 * defecto.
 *
 * <p>Cada alta/baja/modificacion revierte la configuracion vigente a DRAFT
 * para que el cambio no afecte al paciente hasta el siguiente publish
 * explicito.
 *
 * <p>El endpoint reemplaza al anterior {@code /api/admin/ai-agent/guardrails}
 * por motivos semanticos: estas reglas son texto que se concatena al system
 * prompt, NO son guardrails de DigitalOcean (esos viven en
 * {@code /api/admin/ai-agent/provider-guardrails}).
 */
@RestController
@RequestMapping("/api/admin/ai-agent/policy-rules")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Reglas de comportamiento del agente IA. El admin las crea y mantiene (RF31, RF32).")
public class AdminAgentPolicyRuleController {

    private final IAgentPolicyRuleAdminUseCase useCase;

    public AdminAgentPolicyRuleController(IAgentPolicyRuleAdminUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Listar reglas de comportamiento")
    @GetMapping
    public ResponseEntity<List<AgentPolicyRuleResponseDTO>> listRules() {
        List<AgentPolicyRule> all = useCase.listRules();
        return ResponseEntity.ok(AgentPolicyRuleRestMapper.toResponseList(all));
    }

    @Operation(summary = "Crear regla de comportamiento")
    @PostMapping
    public ResponseEntity<AgentPolicyRuleResponseDTO> createRule(
            @Valid @RequestBody AgentPolicyRuleRequestDTO request) {
        AgentPolicyRule saved = useCase.createRule(request.getLabel(), request.getText(), request.isActive());
        return ResponseEntity.status(HttpStatus.CREATED).body(AgentPolicyRuleRestMapper.toResponse(saved));
    }

    @Operation(summary = "Obtener regla por id")
    @GetMapping("/{id}")
    public ResponseEntity<AgentPolicyRuleResponseDTO> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(AgentPolicyRuleRestMapper.toResponse(useCase.getRule(id)));
    }

    @Operation(summary = "Actualizar regla (label, text, active)")
    @PutMapping("/{id}")
    public ResponseEntity<AgentPolicyRuleResponseDTO> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody AgentPolicyRuleRequestDTO request) {
        AgentPolicyRule saved = useCase.updateRule(id, request.getLabel(), request.getText(), request.isActive());
        return ResponseEntity.ok(AgentPolicyRuleRestMapper.toResponse(saved));
    }

    @Operation(summary = "Activar regla",
            description = "Atajo equivalente a PUT con active=true sin cambiar label/text.")
    @PostMapping("/{id}/activate")
    public ResponseEntity<AgentPolicyRuleResponseDTO> activate(@PathVariable Long id) {
        AgentPolicyRule saved = useCase.setRuleActive(id, true);
        return ResponseEntity.ok(AgentPolicyRuleRestMapper.toResponse(saved));
    }

    @Operation(summary = "Desactivar regla",
            description = "Atajo equivalente a PUT con active=false sin cambiar label/text.")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<AgentPolicyRuleResponseDTO> deactivate(@PathVariable Long id) {
        AgentPolicyRule saved = useCase.setRuleActive(id, false);
        return ResponseEntity.ok(AgentPolicyRuleRestMapper.toResponse(saved));
    }

    @Operation(summary = "Borrar regla")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        useCase.deleteRule(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
