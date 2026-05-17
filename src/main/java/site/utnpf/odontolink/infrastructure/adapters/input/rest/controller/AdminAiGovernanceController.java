package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IAiGovernancePolicyUseCase;
import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateAiGovernancePolicyRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiGovernancePolicyResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AiGovernancePolicyRestMapper;

/**
 * Configuracion de la {@link AiGovernancePolicy}: los candados de
 * publicacion del agente (RF31). Endpoint dedicado para que el admin
 * lo administre desde una pantalla separada del editor del agente,
 * en linea con la mitigacion "doble llave" (el override se habilita
 * aqui y se invoca por-publish desde la otra pantalla).
 */
@RestController
@RequestMapping("/api/admin/ai-agent/governance")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Politica de gobernanza para publicar el agente IA (RF31)")
public class AdminAiGovernanceController {

    private final IAiGovernancePolicyUseCase useCase;

    public AdminAiGovernanceController(IAiGovernancePolicyUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Obtener policy vigente")
    @GetMapping
    public ResponseEntity<AiGovernancePolicyResponseDTO> getPolicy() {
        AiGovernancePolicy policy = useCase.getPolicy();
        return ResponseEntity.ok(AiGovernancePolicyRestMapper.toResponse(policy));
    }

    @Operation(summary = "Actualizar policy",
            description = "Cambia los toggles de pre-requisitos para publicar y la posibilidad de " +
                    "override. Cada update se registra en el audit log.")
    @PutMapping
    public ResponseEntity<AiGovernancePolicyResponseDTO> updatePolicy(
            @Valid @RequestBody UpdateAiGovernancePolicyRequestDTO request) {
        AiGovernancePolicy updated = useCase.updatePolicy(
                request.isRequireGuardrails(),
                request.getMinActiveGuardrails(),
                request.isRequireSystemPrompt(),
                request.isRequireWelcomeMessage(),
                request.isRequireIndexedDocuments(),
                request.isAllowOverride()
        );
        return ResponseEntity.ok(AiGovernancePolicyRestMapper.toResponse(updated));
    }
}
