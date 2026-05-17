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
import site.utnpf.odontolink.application.port.in.IGuardrailAdminUseCase;
import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.GuardrailRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.GuardrailResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.GuardrailRestMapper;

import java.util.List;

/**
 * CRUD de guardrails del agente IA (RF32). El admin define todas las
 * reglas de seguridad desde el panel; el sistema NO provee textos por
 * defecto.
 *
 * <p>Cada alta/baja/modificacion revierte la configuracion vigente a
 * DRAFT para que el cambio no afecte al paciente hasta el siguiente
 * publish explicito.
 */
@RestController
@RequestMapping("/api/admin/ai-agent/guardrails")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Agente IA",
        description = "Guardrails clinicos del agente IA. El admin los crea y mantiene (RF32).")
public class AdminGuardrailController {

    private final IGuardrailAdminUseCase useCase;

    public AdminGuardrailController(IGuardrailAdminUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Listar guardrails")
    @GetMapping
    public ResponseEntity<List<GuardrailResponseDTO>> listGuardrails() {
        List<Guardrail> all = useCase.listGuardrails();
        return ResponseEntity.ok(GuardrailRestMapper.toResponseList(all));
    }

    @Operation(summary = "Crear guardrail")
    @PostMapping
    public ResponseEntity<GuardrailResponseDTO> createGuardrail(@Valid @RequestBody GuardrailRequestDTO request) {
        Guardrail saved = useCase.createGuardrail(request.getLabel(), request.getText(), request.isActive());
        return ResponseEntity.status(HttpStatus.CREATED).body(GuardrailRestMapper.toResponse(saved));
    }

    @Operation(summary = "Obtener guardrail por id")
    @GetMapping("/{id}")
    public ResponseEntity<GuardrailResponseDTO> getGuardrail(@PathVariable Long id) {
        return ResponseEntity.ok(GuardrailRestMapper.toResponse(useCase.getGuardrail(id)));
    }

    @Operation(summary = "Actualizar guardrail (label, text, active)")
    @PutMapping("/{id}")
    public ResponseEntity<GuardrailResponseDTO> updateGuardrail(@PathVariable Long id,
                                                                @Valid @RequestBody GuardrailRequestDTO request) {
        Guardrail saved = useCase.updateGuardrail(id, request.getLabel(), request.getText(), request.isActive());
        return ResponseEntity.ok(GuardrailRestMapper.toResponse(saved));
    }

    @Operation(summary = "Activar guardrail",
            description = "Atajo equivalente a PUT con active=true sin cambiar label/text.")
    @PostMapping("/{id}/activate")
    public ResponseEntity<GuardrailResponseDTO> activate(@PathVariable Long id) {
        Guardrail saved = useCase.setGuardrailActive(id, true);
        return ResponseEntity.ok(GuardrailRestMapper.toResponse(saved));
    }

    @Operation(summary = "Desactivar guardrail",
            description = "Atajo equivalente a PUT con active=false sin cambiar label/text.")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<GuardrailResponseDTO> deactivate(@PathVariable Long id) {
        Guardrail saved = useCase.setGuardrailActive(id, false);
        return ResponseEntity.ok(GuardrailRestMapper.toResponse(saved));
    }

    @Operation(summary = "Borrar guardrail")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuardrail(@PathVariable Long id) {
        useCase.deleteGuardrail(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
