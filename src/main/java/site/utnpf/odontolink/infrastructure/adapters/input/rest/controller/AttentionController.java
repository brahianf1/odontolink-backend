package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IAttentionUseCase;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ProgressNoteRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ErrorResponseDTO;
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
 * - GET    /api/practitioner/attentions             - Listar casos del practicante autenticado
 * - GET    /api/patient/attentions                  - Listar casos del paciente autenticado
 *
 * Todos los endpoints están protegidos con @PreAuthorize según el rol requerido.
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Atenciones", description = "Gestión del expediente clínico: detalle del caso, registro de " +
        "evoluciones, finalización del caso (habilitador de feedback) y listados orientados a la vista " +
        "del paciente y del practicante. En los listados por rol, el ID del usuario se infiere del JWT " +
        "para prevenir IDOR.")
@SecurityRequirement(name = "Bearer Authentication")
public class AttentionController {

    private final IAttentionUseCase attentionUseCase;
    private final AuthenticationFacade authenticationFacade;

    public AttentionController(IAttentionUseCase attentionUseCase,
                              AuthenticationFacade authenticationFacade) {
        this.attentionUseCase = attentionUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    // ---------------------------------------------------------------------
    //  ESCRITURA SOBRE EL EXPEDIENTE CLÍNICO
    // ---------------------------------------------------------------------

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
    @Operation(
            summary = "Registrar una nota de evolución (RF11 - CU 4.2)",
            description = "Añade una nueva nota de progreso (evolución clínica) al expediente del caso.\n\n" +
                    "**Permisos de escritura**: este endpoint sólo lo puede invocar el rol " +
                    "`PRACTITIONER` (vía `@PreAuthorize`). El frontend debe ocultar el formulario de " +
                    "redacción de notas para `PATIENT` y para `SUPERVISOR` desde este endpoint; los " +
                    "supervisores cuentan con su propio canal en el módulo de supervisión.\n\n" +
                    "**Autoría**: el autor de la nota se infiere del JWT (no se envía en el body) y " +
                    "queda registrado junto con el `createdAt` del servidor. El campo `content` del body " +
                    "se valida con Bean Validation (longitud 10-5000, no vacío).\n\n" +
                    "**Regla de negocio**: sólo se aceptan notas si el caso está en estado " +
                    "`IN_PROGRESS`. Para casos `COMPLETED` o `CANCELLED` el dominio rechaza la operación " +
                    "y la UI no debería mostrar el formulario.\n\n" +
                    "**Respuesta**: se devuelve la `Attention` completa (no sólo la nota) para que la UI " +
                    "pueda refrescar el expediente sin un GET adicional. El historial de notas se obtiene " +
                    "con `GET /api/attentions/{id}/progress-notes`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Nota registrada correctamente. Se devuelve la `Attention` actualizada.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttentionResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Caso con nueva evolución",
                                    value = """
                                            {
                                              "id": 23,
                                              "status": "IN_PROGRESS",
                                              "startDate": "2025-11-10",
                                              "patientId": 15,
                                              "patientName": "Carlos Rodriguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martinez",
                                              "treatmentId": 3,
                                              "treatmentName": "Limpieza Dental",
                                              "appointments": [
                                                {
                                                  "id": 45,
                                                  "appointmentTime": "2025-11-15T10:00:00",
                                                  "motive": "Control de rutina semestral.",
                                                  "status": "COMPLETED",
                                                  "durationInMinutes": 45,
                                                  "cancellationReason": null,
                                                  "treatmentId": 3,
                                                  "treatmentName": "Limpieza Dental",
                                                  "patientId": 15,
                                                  "patientName": "Carlos Rodriguez",
                                                  "practitionerId": 8,
                                                  "practitionerName": "Ana Martinez",
                                                  "attentionId": 23
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean Validation falló: `content` vacío o fuera del rango 10-5000 caracteres.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Contenido inválido",
                                    value = """
                                            {
                                              "timestamp": "2025-11-15T10:30:00",
                                              "status": 400,
                                              "error": "Validation Error",
                                              "message": "Los datos proporcionados no son válidos",
                                              "path": "/api/attentions/23/progress-notes",
                                              "details": [
                                                "content: La nota debe tener entre 10 y 5000 caracteres"
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol `PRACTITIONER`.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una atención con el `attentionId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Contenido textual de la evolución a registrar.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProgressNoteRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Evolución clínica",
                            value = """
                                    {
                                      "content": "Se realiza profilaxis completa. Paciente tolera bien el procedimiento. Se indica control en 6 meses y refuerzo de técnica de cepillado."
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/attentions/{attentionId}/progress-notes")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<AttentionResponseDTO> addProgressNote(
            @Parameter(description = "ID del caso clínico al que se añade la evolución.",
                    example = "23", required = true)
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

    @Operation(
            summary = "Finalizar el caso clínico (RF10, RF19 - CU 4.4)",
            description = "Cambia el estado del caso a `COMPLETED`. Esta es la transición de cierre " +
                    "definitivo del expediente y **habilita el flujo de Feedback** para paciente y " +
                    "practicante (RF19): hasta que el caso no se finalice, los endpoints de feedback " +
                    "rechazarán el envío.\n\n" +
                    "**Ownership**: sólo el practicante responsable del caso puede invocarlo. Si el " +
                    "usuario autenticado no coincide con `attention.practitioner.user`, la respuesta es " +
                    "**403 Forbidden** con el cuerpo estándar de error.\n\n" +
                    "**Reglas de negocio (RF10)**: la finalización se bloquea con **422 Unprocessable " +
                    "Entity** (`InvalidBusinessRuleException`) en los siguientes escenarios:\n" +
                    "- Existe al menos un turno con `status = SCHEDULED` cuya fecha/hora es futura. El " +
                    "  practicante debe completar o cancelar esos turnos antes de finalizar.\n" +
                    "- Existe al menos un turno con `status = SCHEDULED` cuya fecha/hora ya pasó. El " +
                    "  practicante debe marcar la asistencia (`COMPLETED` o `NO_SHOW`) antes de finalizar.\n\n" +
                    "**Idempotencia**: si el caso ya está en `COMPLETED` o `CANCELLED`, el dominio " +
                    "rechaza la transición. El frontend debe ocultar el botón \"Finalizar\" para casos " +
                    "que no estén en `IN_PROGRESS`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Atención finalizada exitosamente. El estado pasa a `COMPLETED` y se " +
                            "habilita el envío de feedback.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttentionResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Caso finalizado",
                                    value = """
                                            {
                                              "id": 23,
                                              "status": "COMPLETED",
                                              "startDate": "2025-11-10",
                                              "patientId": 15,
                                              "patientName": "Carlos Rodriguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martinez",
                                              "treatmentId": 3,
                                              "treatmentName": "Limpieza Dental",
                                              "appointments": [
                                                {
                                                  "id": 45,
                                                  "appointmentTime": "2025-11-15T10:00:00",
                                                  "motive": "Control de rutina semestral.",
                                                  "status": "COMPLETED",
                                                  "durationInMinutes": 45,
                                                  "cancellationReason": null,
                                                  "treatmentId": 3,
                                                  "treatmentName": "Limpieza Dental",
                                                  "patientId": 15,
                                                  "patientName": "Carlos Rodriguez",
                                                  "practitionerId": 8,
                                                  "practitionerName": "Ana Martinez",
                                                  "attentionId": 23
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El practicante autenticado no es el responsable del caso, o el " +
                            "usuario no posee el rol `PRACTITIONER`.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una atención con el `attentionId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Violación de regla de negocio: el caso aún tiene turnos `SCHEDULED` " +
                            "(futuros o pasados sin marcar) y no puede finalizarse. La UI debería " +
                            "redirigir al usuario al detalle del caso para resolver esos turnos primero.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Turnos futuros agendados",
                                            value = """
                                                    {
                                                      "timestamp": "2025-11-15T11:00:00",
                                                      "status": 422,
                                                      "error": "Business Rule Violation",
                                                      "message": "No se puede finalizar el caso clínico. Aún existen turnos futuros agendados. Por favor, complete o cancele todos los turnos pendientes antes de finalizar el caso.",
                                                      "path": "/api/attentions/23/finalize"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Turnos pasados sin marcar",
                                            value = """
                                                    {
                                                      "timestamp": "2025-11-15T11:00:00",
                                                      "status": 422,
                                                      "error": "Business Rule Violation",
                                                      "message": "No se puede finalizar el caso clínico. Existen turnos pasados que aún no han sido marcados como completados o ausentes. Por favor, revise el historial de turnos y marque la asistencia correspondiente.",
                                                      "path": "/api/attentions/23/finalize"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/attentions/{attentionId}/finalize")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<AttentionResponseDTO> finalizeAttention(
            @Parameter(description = "ID del caso clínico a finalizar.", example = "23", required = true)
            @PathVariable Long attentionId) {

        // Obtener el usuario autenticado (practicante)
        User practitionerUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso (servicio de aplicación)
        Attention attention = attentionUseCase.finalizeAttention(attentionId, practitionerUser);

        // Convertir a DTO de respuesta
        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------
    //  LECTURA DEL EXPEDIENTE CLÍNICO
    // ---------------------------------------------------------------------

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
    @Operation(
            summary = "Obtener el detalle de un caso clínico",
            description = "Devuelve el expediente completo de la atención: estado, fecha de inicio, " +
                    "paciente, practicante, tratamiento y lista de turnos asociados. Es el endpoint " +
                    "principal para la vista de detalle del caso en el frontend.\n\n" +
                    "**Permisos de lectura**: pueden invocarlo los roles `PRACTITIONER`, `PATIENT` y " +
                    "`SUPERVISOR`. Está pensado como recurso compartido del expediente: la UI debe " +
                    "filtrar las acciones visibles según el rol (p. ej. el formulario de evolución y " +
                    "el botón de finalizar son exclusivos del practicante)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la atención.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttentionResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Caso en curso",
                                    value = """
                                            {
                                              "id": 23,
                                              "status": "IN_PROGRESS",
                                              "startDate": "2025-11-10",
                                              "patientId": 15,
                                              "patientName": "Carlos Rodriguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martinez",
                                              "treatmentId": 3,
                                              "treatmentName": "Limpieza Dental",
                                              "appointments": [
                                                {
                                                  "id": 45,
                                                  "appointmentTime": "2025-11-15T10:00:00",
                                                  "motive": "Control de rutina semestral.",
                                                  "status": "SCHEDULED",
                                                  "durationInMinutes": 45,
                                                  "cancellationReason": null,
                                                  "treatmentId": 3,
                                                  "treatmentName": "Limpieza Dental",
                                                  "patientId": 15,
                                                  "patientName": "Carlos Rodriguez",
                                                  "practitionerId": 8,
                                                  "practitionerName": "Ana Martinez",
                                                  "attentionId": 23
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee ninguno de los roles `PRACTITIONER`, " +
                            "`PATIENT` o `SUPERVISOR`.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una atención con el `attentionId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/attentions/{attentionId}")
    @PreAuthorize("hasAnyRole('PRACTITIONER', 'PATIENT', 'SUPERVISOR')")
    public ResponseEntity<AttentionResponseDTO> getAttentionById(
            @Parameter(description = "ID del caso clínico.", example = "23", required = true)
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
    @Operation(
            summary = "Listar las evoluciones de un caso clínico",
            description = "Devuelve el historial cronológico de notas de progreso registradas en el " +
                    "caso, incluyendo autor y rol (`PRACTITIONER`, `SUPERVISOR`, ...).\n\n" +
                    "**Permisos de lectura**: pueden invocarlo los roles `PRACTITIONER`, `PATIENT` y " +
                    "`SUPERVISOR`. La escritura está restringida al rol `PRACTITIONER` (ver " +
                    "`POST /api/attentions/{id}/progress-notes`), por lo que el frontend debe **mostrar** " +
                    "el listado a los tres roles y **ocultar el formulario** de registro a paciente y " +
                    "supervisor desde este endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de evoluciones, posiblemente vacío.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProgressNoteResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "Historial de evoluciones",
                                    value = """
                                            [
                                              {
                                                "id": 101,
                                                "note": "Se realiza profilaxis completa. Paciente tolera bien el procedimiento. Se indica control en 6 meses y refuerzo de técnica de cepillado.",
                                                "createdAt": "2025-11-15T13:45:00Z",
                                                "authorId": 8,
                                                "authorName": "Ana Martinez",
                                                "authorRole": "ROLE_PRACTITIONER",
                                                "attentionId": 23
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee ninguno de los roles `PRACTITIONER`, " +
                            "`PATIENT` o `SUPERVISOR`.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una atención con el `attentionId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/attentions/{attentionId}/progress-notes")
    @PreAuthorize("hasAnyRole('PRACTITIONER', 'PATIENT', 'SUPERVISOR')")
    public ResponseEntity<List<ProgressNoteResponseDTO>> getProgressNotes(
            @Parameter(description = "ID del caso clínico cuyas evoluciones se consultan.",
                    example = "23", required = true)
            @PathVariable Long attentionId) {

        List<ProgressNote> progressNotes = attentionUseCase.getProgressNotesByAttention(attentionId);

        List<ProgressNoteResponseDTO> response = progressNotes.stream()
                .map(ProgressNoteRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------
    //  LISTADOS POR USUARIO AUTENTICADO (resolución del ID vía JWT)
    // ---------------------------------------------------------------------

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
    @Operation(
            summary = "Listar mis casos clínicos (practicante)",
            description = "Devuelve todas las atenciones del practicante autenticado, en cualquier " +
                    "estado (`IN_PROGRESS`, `COMPLETED`, `CANCELLED`). Útil para la vista \"Mis casos\".\n\n" +
                    "**Seguridad (prevención de IDOR)**: el `practitionerId` **no se recibe por query " +
                    "ni por path**: se resuelve internamente desde el token JWT a través de " +
                    "`AuthenticationFacade.getAuthenticatedPractitionerId()`. Esto garantiza que un " +
                    "practicante nunca pueda consultar la cartera de casos de otro practicante " +
                    "manipulando la URL."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de casos del practicante, posiblemente vacío.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AttentionResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "Cartera del practicante",
                                    value = """
                                            [
                                              {
                                                "id": 23,
                                                "status": "IN_PROGRESS",
                                                "startDate": "2025-11-10",
                                                "patientId": 15,
                                                "patientName": "Carlos Rodriguez",
                                                "practitionerId": 8,
                                                "practitionerName": "Ana Martinez",
                                                "treatmentId": 3,
                                                "treatmentName": "Limpieza Dental",
                                                "appointments": []
                                              },
                                              {
                                                "id": 24,
                                                "status": "COMPLETED",
                                                "startDate": "2025-10-02",
                                                "patientId": 17,
                                                "patientName": "Lucia Fernandez",
                                                "practitionerId": 8,
                                                "practitionerName": "Ana Martinez",
                                                "treatmentId": 5,
                                                "treatmentName": "Endodoncia",
                                                "appointments": []
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol `PRACTITIONER`.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El usuario autenticado no tiene un perfil de `Practitioner` asociado " +
                            "(inconsistencia de cuenta).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
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
    @Operation(
            summary = "Listar mis casos clínicos (paciente)",
            description = "Devuelve todas las atenciones del paciente autenticado, en cualquier estado " +
                    "(`IN_PROGRESS`, `COMPLETED`, `CANCELLED`). Útil para la vista \"Mi historial clínico\".\n\n" +
                    "**Seguridad (prevención de IDOR)**: el `patientId` **no se recibe por query ni " +
                    "por path**: se resuelve internamente desde el token JWT a través de " +
                    "`AuthenticationFacade.getAuthenticatedPatientId()`. Esto garantiza que un paciente " +
                    "nunca pueda consultar el historial de otro paciente manipulando la URL."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de casos del paciente, posiblemente vacío.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AttentionResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "Historial del paciente",
                                    value = """
                                            [
                                              {
                                                "id": 23,
                                                "status": "IN_PROGRESS",
                                                "startDate": "2025-11-10",
                                                "patientId": 15,
                                                "patientName": "Carlos Rodriguez",
                                                "practitionerId": 8,
                                                "practitionerName": "Ana Martinez",
                                                "treatmentId": 3,
                                                "treatmentName": "Limpieza Dental",
                                                "appointments": []
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol `PATIENT`.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El usuario autenticado no tiene un perfil de `Patient` asociado " +
                            "(inconsistencia de cuenta).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
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
