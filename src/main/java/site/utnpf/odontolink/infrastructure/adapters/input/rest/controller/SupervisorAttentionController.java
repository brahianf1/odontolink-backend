package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.ISupervisorAttentionUseCase;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ProgressNoteResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AttentionRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ProgressNoteRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de entrada REST para RF39 - Auditoría y Supervisión de Atenciones por parte del Docente.
 *
 * Las rutas cuelgan de /api/supervisors/my-practitioners/{practitionerId}/... para mantener
 * la jerarquía semántica iniciada en {@link SupervisorController}: el "recurso raíz" del
 * supervisor son los practicantes a su cargo, y dentro de cada uno vive su expediente clínico
 * (Atenciones, evoluciones).
 *
 * Capas de defensa:
 *  - Filtros de seguridad y JWT delegados a la cadena de Spring Security.
 *  - {@code @PreAuthorize("hasRole('SUPERVISOR')")} en cada endpoint.
 *  - {@link ISupervisorAttentionUseCase} aplica el cerco docente-alumno y la coherencia
 *    practicante↔atención en el dominio.
 */
@RestController
@RequestMapping("/api/supervisors/my-practitioners")
@Tag(
    name = "Supervisión Clínica",
    description = "RF39 - Auditoría del expediente clínico de los practicantes vinculados al supervisor"
)
public class SupervisorAttentionController {

    private final ISupervisorAttentionUseCase supervisorAttentionUseCase;
    private final AuthenticationFacade authenticationFacade;

    public SupervisorAttentionController(ISupervisorAttentionUseCase supervisorAttentionUseCase,
                                         AuthenticationFacade authenticationFacade) {
        this.supervisorAttentionUseCase = supervisorAttentionUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Lista todas las atenciones de un practicante vinculado al supervisor autenticado.
     *
     * GET /api/supervisors/my-practitioners/{practitionerId}/attentions
     */
    @GetMapping("/{practitionerId}/attentions")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Listar atenciones de un practicante a cargo",
        description = "Devuelve las atenciones (casos clínicos) del practicante vinculado. " +
                      "Si el practicante no está vinculado al supervisor autenticado, responde 403."
    )
    public ResponseEntity<List<AttentionResponseDTO>> listPractitionerAttentions(
            @Parameter(description = "ID del practicante a cargo", required = true)
            @PathVariable Long practitionerId) {

        User supervisorUser = authenticationFacade.getAuthenticatedUser();
        List<Attention> attentions = supervisorAttentionUseCase
                .getPractitionerAttentions(practitionerId, supervisorUser);

        List<AttentionResponseDTO> response = attentions.stream()
                .map(AttentionRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Devuelve el detalle de una atención específica (turnos y notas incluidos).
     *
     * GET /api/supervisors/my-practitioners/{practitionerId}/attentions/{attentionId}
     */
    @GetMapping("/{practitionerId}/attentions/{attentionId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Auditar el detalle de una atención",
        description = "Devuelve la atención completa (turnos y evoluciones) para auditoría. " +
                      "Valida que la atención pertenezca al practicante indicado y que exista " +
                      "vínculo de supervisión activo."
    )
    public ResponseEntity<AttentionResponseDTO> getPractitionerAttentionDetail(
            @Parameter(description = "ID del practicante a cargo", required = true)
            @PathVariable Long practitionerId,
            @Parameter(description = "ID de la atención a auditar", required = true)
            @PathVariable Long attentionId) {

        User supervisorUser = authenticationFacade.getAuthenticatedUser();
        Attention attention = supervisorAttentionUseCase
                .getPractitionerAttentionDetail(practitionerId, attentionId, supervisorUser);

        return ResponseEntity.ok(AttentionRestMapper.toResponse(attention));
    }

    /**
     * Devuelve las notas de progreso (evoluciones) de una atención de un practicante vinculado.
     *
     * GET /api/supervisors/my-practitioners/{practitionerId}/attentions/{attentionId}/progress-notes
     */
    @GetMapping("/{practitionerId}/attentions/{attentionId}/progress-notes")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Auditar notas de evolución de una atención",
        description = "Devuelve la lista de notas de evolución clínica registradas en la atención " +
                      "del practicante vinculado."
    )
    public ResponseEntity<List<ProgressNoteResponseDTO>> getPractitionerAttentionProgressNotes(
            @Parameter(description = "ID del practicante a cargo", required = true)
            @PathVariable Long practitionerId,
            @Parameter(description = "ID de la atención a auditar", required = true)
            @PathVariable Long attentionId) {

        User supervisorUser = authenticationFacade.getAuthenticatedUser();
        List<ProgressNote> notes = supervisorAttentionUseCase
                .getPractitionerAttentionProgressNotes(practitionerId, attentionId, supervisorUser);

        List<ProgressNoteResponseDTO> response = notes.stream()
                .map(ProgressNoteRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Finalización por autoridad académica: el docente cierra la carpeta del caso.
     *
     * POST /api/supervisors/my-practitioners/{practitionerId}/attentions/{attentionId}/finalize
     */
    @PostMapping("/{practitionerId}/attentions/{attentionId}/finalize")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Finalizar atención por autoridad académica",
        description = "Cambia el estado de la atención a COMPLETED ejerciendo la potestad académica " +
                      "del supervisor. Aplica las mismas reglas clínicas que la finalización del " +
                      "practicante (sin turnos futuros pendientes ni pasados sin marcar) y requiere " +
                      "vínculo de supervisión activo."
    )
    public ResponseEntity<AttentionResponseDTO> finalizeAttentionAsSupervisor(
            @Parameter(description = "ID del practicante a cargo", required = true)
            @PathVariable Long practitionerId,
            @Parameter(description = "ID de la atención a cerrar", required = true)
            @PathVariable Long attentionId) {

        User supervisorUser = authenticationFacade.getAuthenticatedUser();
        Attention attention = supervisorAttentionUseCase
                .finalizeAttentionAsSupervisor(practitionerId, attentionId, supervisorUser);

        return ResponseEntity.ok(AttentionRestMapper.toResponse(attention));
    }
}
