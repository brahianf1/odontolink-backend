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
import site.utnpf.odontolink.application.port.in.IInstitutionalSettingsUseCase;
import site.utnpf.odontolink.domain.model.InstitutionalSettings;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateInstitutionalSettingsRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.InstitutionalSettingsResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.InstitutionalSettingsRestMapper;

/**
 * Adaptador de entrada REST para los parámetros institucionales (RF07).
 *
 * Expone una vista (GET) y un comando de reemplazo total (PUT) sobre la
 * configuración singleton. La restricción a {@code ROLE_ADMIN} se aplica
 * tanto a nivel de {@link PreAuthorize} como en la cadena declarativa de
 * {@code SecurityConfig}, manteniendo defensa en profundidad.
 */
@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administración - Parámetros institucionales",
        description = "Visualización y modificación de parámetros institucionales (RF07)")
public class AdminInstitutionalSettingsController {

    private final IInstitutionalSettingsUseCase institutionalSettingsUseCase;

    public AdminInstitutionalSettingsController(IInstitutionalSettingsUseCase institutionalSettingsUseCase) {
        this.institutionalSettingsUseCase = institutionalSettingsUseCase;
    }

    @Operation(summary = "Obtener parámetros institucionales actuales")
    @GetMapping
    public ResponseEntity<InstitutionalSettingsResponseDTO> getSettings() {
        InstitutionalSettings settings = institutionalSettingsUseCase.getSettings();
        return ResponseEntity.ok(InstitutionalSettingsRestMapper.toDTO(settings));
    }

    @Operation(summary = "Actualizar parámetros institucionales",
            description = "Reemplaza por completo la configuración. Las modificaciones se aplican " +
                    "de forma inmediata, tal como exige el RF07.")
    @PutMapping
    public ResponseEntity<InstitutionalSettingsResponseDTO> updateSettings(
            @Valid @RequestBody UpdateInstitutionalSettingsRequestDTO request) {

        InstitutionalSettings updated = institutionalSettingsUseCase.updateSettings(
                request.getInstitutionName(),
                request.getOpeningHours(),
                request.getUsagePolicies(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getContactAddress(),
                request.getMaxConcurrentAppointmentsPerAttention()
        );
        return ResponseEntity.ok(InstitutionalSettingsRestMapper.toDTO(updated));
    }
}
