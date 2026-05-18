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
import site.utnpf.odontolink.application.port.in.IEmergencyKeywordAdminUseCase;
import site.utnpf.odontolink.domain.model.EmergencyKeyword;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CreateEmergencyKeywordRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateEmergencyKeywordRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.EmergencyKeywordResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.EmergencyKeywordRestMapper;

import java.util.List;

/**
 * Controller admin para el diccionario de emergencias del chatbot (RF32).
 *
 * <p>Sigue el mismo patron que los otros endpoints admin del modulo IA:
 * {@code /api/admin/ai-agent/...} + {@code @PreAuthorize("hasRole('ADMIN')")}.
 */
@RestController
@RequestMapping("/api/admin/ai-agent/emergency-keywords")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Emergency Keywords",
        description = "Diccionario de keywords que disparan derivacion de emergencia en el chatbot (RF32)")
public class AdminEmergencyKeywordController {

    private final IEmergencyKeywordAdminUseCase useCase;

    public AdminEmergencyKeywordController(IEmergencyKeywordAdminUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Listar keywords de emergencia")
    @GetMapping
    public ResponseEntity<List<EmergencyKeywordResponseDTO>> list() {
        List<EmergencyKeywordResponseDTO> body = useCase.listAll().stream()
                .map(EmergencyKeywordRestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Crear keyword de emergencia")
    @PostMapping
    public ResponseEntity<EmergencyKeywordResponseDTO> create(
            @Valid @RequestBody CreateEmergencyKeywordRequestDTO request) {
        EmergencyKeyword created = useCase.create(request.getTerm(), request.isActive());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EmergencyKeywordRestMapper.toResponse(created));
    }

    @Operation(summary = "Actualizar keyword de emergencia")
    @PutMapping("/{id}")
    public ResponseEntity<EmergencyKeywordResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmergencyKeywordRequestDTO request) {
        EmergencyKeyword updated = useCase.update(id, request.getTerm(), request.isActive());
        return ResponseEntity.ok(EmergencyKeywordRestMapper.toResponse(updated));
    }

    @Operation(summary = "Eliminar keyword de emergencia")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
