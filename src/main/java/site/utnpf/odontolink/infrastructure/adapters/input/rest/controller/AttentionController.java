package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IAttentionUseCase;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ProgressNoteRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ProgressNoteResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AttentionRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ProgressNoteRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones de atención (casos clínicos).
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados (Fase 4 - Trazabilidad del Caso Clínico):
 * - POST   /api/attentions/{id}/progress-notes     - Registrar evolución (RF11 - CU 4.2)
 * - POST   /api/attentions/{id}/finalize            - Finalizar caso clínico (RF10, RF19 - CU 4.4)
 * - GET    /api/attentions/{id}                     - Obtener detalle de un caso
 * - GET    /api/attentions/{id}/progress-notes      - Obtener evoluciones de un caso
 * - GET    /api/practitioner/attentions             - Obtener casos del practicante
 *
 * Todos los endpoints están protegidos con @PreAuthorize según el rol requerido.
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api")
public class AttentionController {

    private final IAttentionUseCase attentionUseCase;
    private final AuthenticationFacade authenticationFacade;

    public AttentionController(IAttentionUseCase attentionUseCase,
                              AuthenticationFacade authenticationFacade) {
        this.attentionUseCase = attentionUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Añade una nota de progreso (evolución) a un caso clínico.
     * Implementa RF11 - CU 4.2: Registrar Evolución.
     *
     * Este endpoint permite al practicante registrar el progreso del tratamiento,
     * documentando observaciones, procedimientos realizados y estado del paciente.
     *
     * POST /api/attentions/{id}/progress-notes
     *
     * Seguridad: Solo PRACTITIONER puede acceder
     *
     * @param attentionId ID del caso clínico
     * @param request DTO con el contenido de la nota
     * @return La Attention actualizada con la nueva ProgressNote
     */
    @PostMapping("/attentions/{attentionId}/progress-notes")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<AttentionResponseDTO> addProgressNote(
            @PathVariable Long attentionId,
            @Valid @RequestBody ProgressNoteRequestDTO request) {

        // Obtener el usuario autenticado (practicante)
        User authorUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso (servicio de aplicación)
        Attention attention = attentionUseCase.addProgressNoteToAttention(
                attentionId,
                request.getContent(),
                authorUser
        );

        // Convertir a DTO de respuesta
        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.ok(response);
    }

    /**
     * Finaliza un caso clínico completo, cambiando su estado a COMPLETED.
     * Implementa RF10, RF19 - CU 4.4: Finalizar Caso Clínico.
     *
     * Este endpoint permite al practicante cerrar el caso una vez completado el tratamiento.
     * Al finalizar, se habilita la funcionalidad de feedback (RF21, RF22).
     *
     * POST /api/attentions/{id}/finalize
     *
     * Seguridad: Solo PRACTITIONER puede acceder
     *
     * Validaciones (aplicadas por el servicio de dominio):
     * - No deben existir turnos futuros agendados
     * - Todos los turnos pasados deben estar marcados (COMPLETED o NO_SHOW)
     *
     * @param attentionId ID del caso clínico a finalizar
     * @return La Attention actualizada con estado COMPLETED
     */
    @PostMapping("/attentions/{attentionId}/finalize")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<AttentionResponseDTO> finalizeAttention(
            @PathVariable Long attentionId) {

        // Obtener el usuario autenticado (practicante)
        User practitionerUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso (servicio de aplicación)
        Attention attention = attentionUseCase.finalizeAttention(attentionId, practitionerUser);

        // Convertir a DTO de respuesta
        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene el detalle de un caso clínico específico.
     *
     * GET /api/attentions/{id}
     *
     * Seguridad: PRACTITIONER, PATIENT y SUPERVISOR pueden acceder
     *
     * @param attentionId ID del caso clínico
     * @return El detalle completo del caso
     */
    @GetMapping("/attentions/{attentionId}")
    @PreAuthorize("hasAnyRole('PRACTITIONER', 'PATIENT', 'SUPERVISOR')")
    public ResponseEntity<AttentionResponseDTO> getAttentionById(
            @PathVariable Long attentionId) {

        Attention attention = attentionUseCase.getAttentionById(attentionId);

        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene las notas de progreso (evoluciones) de un caso clínico.
     * Permite consultar el historial de evoluciones documentadas.
     *
     * GET /api/attentions/{id}/progress-notes
     *
     * Seguridad: PRACTITIONER, PATIENT y SUPERVISOR pueden acceder
     *
     * @param attentionId ID del caso clínico
     * @return Lista de notas de progreso del caso
     */
    @GetMapping("/attentions/{attentionId}/progress-notes")
    @PreAuthorize("hasAnyRole('PRACTITIONER', 'PATIENT', 'SUPERVISOR')")
    public ResponseEntity<List<ProgressNoteResponseDTO>> getProgressNotes(
            @PathVariable Long attentionId) {

        List<ProgressNote> progressNotes = attentionUseCase.getProgressNotesByAttention(attentionId);

        List<ProgressNoteResponseDTO> response = progressNotes.stream()
                .map(ProgressNoteRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todos los casos clínicos del practicante autenticado.
     * Permite al practicante ver su lista de casos (activos y finalizados).
     *
     * GET /api/practitioner/attentions
     *
     * Seguridad: Solo PRACTITIONER puede acceder
     *
     * @return Lista de atenciones del practicante
     */
    @GetMapping("/practitioner/attentions")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<List<AttentionResponseDTO>> getMyAttentions() {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();

        List<Attention> attentions = attentionUseCase.getAttentionsByPractitioner(practitionerId);

        List<AttentionResponseDTO> response = attentions.stream()
                .map(AttentionRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todos los casos clínicos del paciente autenticado.
     * Permite al paciente ver su historial de atenciones.
     *
     * GET /api/patient/attentions
     *
     * Seguridad: Solo PATIENT puede acceder
     *
     * @return Lista de atenciones del paciente
     */
    @GetMapping("/patient/attentions")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AttentionResponseDTO>> getMyAttentionsAsPatient() {

        Long patientId = authenticationFacade.getAuthenticatedPatientId();

        List<Attention> attentions = attentionUseCase.getAttentionsByPatient(patientId);

        List<AttentionResponseDTO> response = attentions.stream()
                .map(AttentionRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
