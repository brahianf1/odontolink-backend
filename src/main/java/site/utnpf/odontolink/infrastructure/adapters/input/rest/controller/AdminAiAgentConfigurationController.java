package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IAiAgentConfigurationUseCase;
import site.utnpf.odontolink.application.port.in.IAiAgentConfigurationUseCase.HealthResult;
import site.utnpf.odontolink.application.port.in.IAiAgentConfigurationUseCase.PreviewResult;
import site.utnpf.odontolink.application.port.in.IAgentPolicyRuleAdminUseCase;
import site.utnpf.odontolink.application.service.AiAgentConfigurationService;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateAiAgentConfigurationRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentConfigurationResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentHealthResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentInstructionPreviewResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AiAgentConfigurationRestMapper;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de entrada REST para la configuracion del agente IA
 * (RF31, RF32). Expone el ciclo completo: leer / crear-actualizar /
 * publicar / preview / health / revertir a DRAFT.
 */
@RestController
@RequestMapping("/api/admin/ai-agent/configuration")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Configuracion del agente IA: prompts, parametros, ciclo de vida (RF31, RF32)")
public class AdminAiAgentConfigurationController {

    private final IAiAgentConfigurationUseCase configUseCase;
    private final IAgentPolicyRuleAdminUseCase policyRuleUseCase;
    private final AiAgentConfigurationService configService;

    public AdminAiAgentConfigurationController(IAiAgentConfigurationUseCase configUseCase,
                                               IAgentPolicyRuleAdminUseCase policyRuleUseCase,
                                               AiAgentConfigurationService configService) {
        this.configUseCase = configUseCase;
        this.policyRuleUseCase = policyRuleUseCase;
        // Inyectamos la implementacion concreta solo para acceder a la
        // operacion administrativa "clear cache" que no esta en el puerto.
        this.configService = configService;
    }

    @Operation(summary = "Obtener configuracion vigente",
            description = "Devuelve la configuracion actual con el preview de la instruccion final " +
                    "(guardrails activos concatenados + systemPromptCore). Si nunca se configuro, " +
                    "responde 204 No Content (lifecycle virtual UNCONFIGURED).")
    @GetMapping
    public ResponseEntity<AiAgentConfigurationResponseDTO> getConfiguration() {
        Optional<AiAgentConfiguration> opt = configUseCase.findConfiguration();
        if (opt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<AgentPolicyRule> activeRules = policyRuleUseCase.listRules().stream()
                .filter(AgentPolicyRule::isActive)
                .toList();
        return ResponseEntity.ok(AiAgentConfigurationRestMapper.toResponse(opt.get(), activeRules));
    }

    @Operation(summary = "Crear o actualizar configuracion",
            description = "Si no existe, crea la fila en DRAFT. Si existe, actualiza y revierte a DRAFT. " +
                    "No sincroniza con el proveedor: para eso usar POST /publish.")
    @PutMapping
    public ResponseEntity<AiAgentConfigurationResponseDTO> saveConfiguration(
            @Valid @RequestBody UpdateAiAgentConfigurationRequestDTO request) {
        AiAgentConfiguration saved = configUseCase.saveConfiguration(
                AiAgentConfigurationRestMapper.toCommand(request));
        List<AgentPolicyRule> activeRules = policyRuleUseCase.listRules().stream()
                .filter(AgentPolicyRule::isActive)
                .toList();
        return ResponseEntity.ok(AiAgentConfigurationRestMapper.toResponse(saved, activeRules));
    }

    @Operation(summary = "Publicar el agente al proveedor",
            description = "Aplica los checks de gobernanza y sincroniza la instruccion final " +
                    "(guardrails + prompt) al proveedor. Si faltan requisitos: 422 con detalle. " +
                    "Si la policy permite override y se invoca con ?override=true, salta los checks " +
                    "y queda auditado en el versionado y en el audit log.")
    @PostMapping("/publish")
    public ResponseEntity<AiAgentConfigurationResponseDTO> publish(
            @RequestParam(name = "override", defaultValue = "false") boolean override) {
        AiAgentConfiguration published = configUseCase.publish(override);
        List<AgentPolicyRule> activeRules = policyRuleUseCase.listRules().stream()
                .filter(AgentPolicyRule::isActive)
                .toList();
        return ResponseEntity.ok(AiAgentConfigurationRestMapper.toResponse(published, activeRules));
    }

    @Operation(summary = "Revertir a DRAFT sin tocar al proveedor",
            description = "Pone la configuracion vigente en DRAFT. No re-publica nada hasta que se " +
                    "invoque /publish.")
    @PostMapping("/revert-to-draft")
    public ResponseEntity<AiAgentConfigurationResponseDTO> revertToDraft() {
        AiAgentConfiguration reverted = configUseCase.revertToDraft();
        List<AgentPolicyRule> activeRules = policyRuleUseCase.listRules().stream()
                .filter(AgentPolicyRule::isActive)
                .toList();
        return ResponseEntity.ok(AiAgentConfigurationRestMapper.toResponse(reverted, activeRules));
    }

    @Operation(summary = "Preview de la instruccion final",
            description = "Devuelve el texto exacto que viajaria al proveedor (guardrails activos + " +
                    "systemPromptCore). No toca al proveedor.")
    @GetMapping("/preview")
    public ResponseEntity<AiAgentInstructionPreviewResponseDTO> preview() {
        PreviewResult result = configUseCase.preview();
        return ResponseEntity.ok(new AiAgentInstructionPreviewResponseDTO(
                result.composedInstruction(),
                result.activeGuardrailLabels()
        ));
    }

    @Operation(summary = "Limpiar cache de URL de invocacion del agente",
            description = "Borra la URL de invocacion cacheada en BD. La proxima request al chatbot " +
                    "fuerza un re-descubrimiento via management API. Util cuando el operador cambio " +
                    "el deployment en el dashboard de DigitalOcean.")
    @PostMapping("/clear-invocation-url-cache")
    public ResponseEntity<AiAgentConfigurationResponseDTO> clearInvocationUrlCache() {
        AiAgentConfiguration updated = configService.clearAgentInvocationUrlCache();
        List<AgentPolicyRule> activeRules = policyRuleUseCase.listRules().stream()
                .filter(AgentPolicyRule::isActive)
                .toList();
        return ResponseEntity.ok(AiAgentConfigurationRestMapper.toResponse(updated, activeRules));
    }

    @Operation(summary = "Health-check del modulo",
            description = "Devuelve lifecycle, requisitos faltantes para publicar y alcanzabilidad " +
                    "del proveedor (pegando un GET liviano al agente). Usado por el FE para pintar " +
                    "el badge 'listo para publicar' / 'configuracion incompleta'.")
    @GetMapping("/health")
    public ResponseEntity<AiAgentHealthResponseDTO> health() {
        HealthResult result = configUseCase.health();
        return ResponseEntity.ok(new AiAgentHealthResponseDTO(
                result.lifecycle() == null ? AiAgentLifecycle.UNCONFIGURED : result.lifecycle(),
                result.missingRequirements(),
                result.providerReachable(),
                result.providerErrorDetail(),
                result.agentInvocationReachable(),
                result.agentInvocationErrorDetail()
        ));
    }
}
