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
 * Endpoints micro-contexto de Feedback. El catálogo de criterios vive en
 * {@code FeedbackCriterionCatalogController}; el panel docente y los charts
 * en los controllers bajo {@code /api/supervisors/feedbacks/}.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Feedback", description = "Encuesta multi-criterio entre paciente y practicante sobre atenciones finalizadas.")
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
            summary = "Crear feedback multi-criterio sobre atención (RF21, RF22, RF23)",
            description = "Permite al paciente o practicante calificar una atención finalizada " +
                    "(`AttentionStatus.COMPLETED`) puntuando un set de criterios definidos en el " +
                    "catálogo (ver `GET /api/feedback/criteria`).\n\n" +
                    "**Forma de la respuesta**: en éxito (`201 Created`) se devuelve el feedback creado " +
                    "incluyendo el array `scores` con la puntuación por criterio. La respuesta no incluye " +
                    "flags derivados de UI (`hasMyFeedback`, contadores, etc.): esos son estado de " +
                    "presentación que el frontend debe derivar.\n\n" +
                    "**Validaciones**: la encuesta debe cubrir EXACTAMENTE el set de criterios activos " +
                    "para la dirección (P→Pr o Pr→Pat). Faltantes, intrusos, duplicados o scores fuera " +
                    "de 1–5 → 400 con detalle. Reintento duplicado por el mismo usuario → 400 (RF23, " +
                    "enforce server-side por `FeedbackPolicyService` + UK en BD).\n\n" +
                    "**Patrón sugerido para el frontend**: tras 201 actualizar estado local " +
                    "(ocultar CTA Calificar) y opcionalmente refetchear el listado de atenciones."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Feedback creado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FeedbackResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 12,
                                              "comment": "Excelente atención",
                                              "createdAt": "2026-05-23T14:30:00Z",
                                              "submittedById": 15,
                                              "submittedByName": "Carlos Rodríguez",
                                              "submittedByRole": "ROLE_PATIENT",
                                              "attentionId": 23,
                                              "treatmentName": "Limpieza Dental",
                                              "patientName": "Carlos Rodríguez",
                                              "practitionerName": "Ana Martínez",
                                              "scores": [
                                                {"criterionCode": "PUNCTUALITY", "criterionDisplayName": "Puntualidad", "score": 5},
                                                {"criterionCode": "CARE_QUALITY", "criterionDisplayName": "Calidad de atención", "score": 4},
                                                {"criterionCode": "COMMUNICATION_CLARITY", "criterionDisplayName": "Claridad en la comunicación", "score": 5},
                                                {"criterionCode": "GENERAL_SATISFACTION", "criterionDisplayName": "Satisfacción general", "score": 5}
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400",
                    description = "Atención no finalizada, encuesta inválida o feedback duplicado",
                    content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Encuesta multi-criterio",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "attentionId": 23,
                                      "comment": "Excelente atención",
                                      "scores": [
                                        {"criterionCode": "PUNCTUALITY", "score": 5},
                                        {"criterionCode": "CARE_QUALITY", "score": 4},
                                        {"criterionCode": "COMMUNICATION_CLARITY", "score": 5},
                                        {"criterionCode": "GENERAL_SATISFACTION", "score": 5}
                                      ]
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/feedback")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<FeedbackResponseDTO> createFeedback(
            @Valid @RequestBody CreateFeedbackRequestDTO request) {

        User submittingUser = authenticationFacade.getAuthenticatedUser();

        Feedback feedback = feedbackUseCase.createFeedback(
                request.getAttentionId(),
                FeedbackRestMapper.toCommand(request.getScores()),
                request.getComment(),
                submittingUser
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeedbackRestMapper.toResponse(feedback));
    }

    @Operation(
            summary = "Listar feedbacks de una atención (RF24)",
            description = "Devuelve el feedback bidireccional de la atención. Visibilidad: paciente y " +
                    "practicante de la atención, y supervisores de ese practicante."
    )
    @GetMapping("/feedback/attention/{attentionId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PRACTITIONER', 'SUPERVISOR')")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackForAttention(
            @PathVariable Long attentionId) {
        User requestingUser = authenticationFacade.getAuthenticatedUser();
        List<Feedback> feedbacks = feedbackUseCase.getFeedbackForAttention(attentionId, requestingUser);
        List<FeedbackResponseDTO> response = feedbacks.stream()
                .map(FeedbackRestMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
