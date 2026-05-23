package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IFeedbackUseCase;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CreateFeedbackRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.FeedbackResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.FeedbackRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones MICRO-contexto de Feedback.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - POST   /api/feedback                              - Crear feedback (CU-009, CU-016: RF21, RF22, RF23)
 * - GET    /api/feedback/attention/{attentionId}      - Ver feedback de una atención (CU-010: RF24)
 *
 * El MACRO-contexto del docente (Panel de Supervisión de Feedback RF25) vive en
 * {@code SupervisorFeedbackDashboardController}: el viejo endpoint
 * {@code GET /api/supervisor/feedback/practitioner/{practitionerId}} fue
 * absorbido por {@code GET /api/supervisors/feedbacks/dashboard}, que ofrece
 * filtros combinables, paginación y agregados, además del mismo cerco
 * docente-alumno.
 *
 * Todos los endpoints están protegidos con @PreAuthorize según el rol requerido.
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Feedback", description = "Sistema de retroalimentación y calificación de atenciones entre pacientes y practicantes")
@SecurityRequirement(name = "Bearer Authentication")
public class FeedbackController {

    private final IFeedbackUseCase feedbackUseCase;
    private final AuthenticationFacade authenticationFacade;

    public FeedbackController(IFeedbackUseCase feedbackUseCase,
                             AuthenticationFacade authenticationFacade) {
        this.feedbackUseCase = feedbackUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    @Operation(
            summary = "Crear feedback sobre atención (RF21, RF22, RF23 - CU-009, CU-016)",
            description = "Permite al paciente o practicante calificar una atención finalizada " +
                    "(`AttentionStatus.COMPLETED`).\n\n" +
                    "**Forma de la respuesta**: en éxito (`201 Created`) se devuelve únicamente el " +
                    "`FeedbackResponseDTO` del feedback recién creado. Por diseño, la respuesta no incluye " +
                    "la `Attention` actualizada ni flags derivados de UI (`hasMyFeedback`, `feedbackCount`, " +
                    "etc.): esos son estados de presentación que el frontend debe derivar de sus propios " +
                    "datos. Mezclarlos aquí acoplaría el contrato REST a vistas específicas del cliente.\n\n" +
                    "**Garantía de unicidad (RF23)**: la regla \"un usuario emite a lo sumo un feedback por " +
                    "atención\" se aplica server-side en `FeedbackPolicyService.validateFeedbackCreation` " +
                    "vía `existsByAttentionAndSubmittedBy`. Un reintento duplicado responde `400` con el " +
                    "código de error de regla de negocio violada. Esa validación es el cinturón real; no " +
                    "se necesita un flag adicional en la respuesta para sostener la UX.\n\n" +
                    "**Patrón sugerido para el frontend**: tras un `201 Created` actualizar el estado " +
                    "local (optimistic update: ocultar el botón \"Calificar\" para esa atención, sumar al " +
                    "contador propio) y, si la vista lo requiere, invalidar/refetchear el listado de " +
                    "atenciones (`GET /api/practitioner/attentions` o `GET /api/patient/attentions`) y/o " +
                    "`GET /api/feedback/attention/{attentionId}` para consolidar consistencia."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Feedback creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FeedbackResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 12,
                                              "rating": 5,
                                              "comment": "Excelente atención, muy profesional y cuidadoso",
                                              "createdAt": "2025-11-20T14:30:00Z",
                                              "submittedById": 15,
                                              "submittedByName": "Carlos Rodríguez",
                                              "submittedByRole": "ROLE_PATIENT",
                                              "attentionId": 23,
                                              "treatmentName": "Limpieza Dental",
                                              "patientName": "Carlos Rodríguez",
                                              "practitionerName": "Ana Martínez"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Atención no finalizada o feedback ya existe",
                    content = @Content(mediaType = "application/json")
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del feedback",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "attentionId": 23,
                                      "rating": 5,
                                      "comment": "Excelente atención, muy profesional y cuidadoso"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/feedback")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<FeedbackResponseDTO> createFeedback(
            @Valid @RequestBody CreateFeedbackRequestDTO request) {

        // Obtener el usuario autenticado (paciente o practicante)
        User submittingUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso (servicio de aplicación)
        Feedback feedback = feedbackUseCase.createFeedback(
                request.getAttentionId(),
                request.getRating(),
                request.getComment(),
                submittingUser
        );

        // Convertir a DTO de respuesta
        FeedbackResponseDTO response = FeedbackRestMapper.toResponse(feedback);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene todos los feedbacks asociados a una atención específica.
     * Implementa CU-010 (RF24) - Para pacientes y practicantes.
     *
     * Este endpoint permite a los pacientes y practicantes involucrados en una atención
     * consultar el feedback bidireccional de esa atención.
     *
     * GET /api/feedback/attention/{attentionId}
     *
     * Seguridad: PATIENT, PRACTITIONER y SUPERVISOR pueden acceder
     *
     * Regla de privacidad:
     * - Solo el paciente o practicante de la atención puede consultar su feedback
     * - Los supervisores pueden consultar el feedback de las atenciones de sus practicantes
     *
     * @param attentionId ID de la atención
     * @return Lista de feedbacks de la atención
     */
    @GetMapping("/feedback/attention/{attentionId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PRACTITIONER', 'SUPERVISOR')")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackForAttention(
            @PathVariable Long attentionId) {

        // Obtener el usuario autenticado
        User requestingUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso (servicio de aplicación)
        // El servicio validará los permisos de acceso
        List<Feedback> feedbacks = feedbackUseCase.getFeedbackForAttention(attentionId, requestingUser);

        // Convertir a DTOs de respuesta
        List<FeedbackResponseDTO> response = feedbacks.stream()
                .map(FeedbackRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
