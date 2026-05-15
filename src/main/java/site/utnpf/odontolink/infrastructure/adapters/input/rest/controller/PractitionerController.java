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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentDeletionResult;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AddOfferedTreatmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CancelAppointmentByPractitionerRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateOfferedTreatmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AppointmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ErrorResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentDeletionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AppointmentRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AvailabilitySlotInputMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.OfferedTreatmentRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión del catálogo personal de tratamientos del practicante.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints de Catálogo:
 * - POST   /api/practitioner/offered-treatments          - Agregar tratamiento (CU-005)
 * - GET    /api/practitioner/offered-treatments          - Obtener mi catálogo
 * - PUT    /api/practitioner/offered-treatments/{id}     - Modificar tratamiento (CU-006)
 * - DELETE /api/practitioner/offered-treatments/{id}     - Eliminar tratamiento (CU-007 / RF16)
 *
 * Endpoints de Turnos:
 * - GET    /api/practitioner/appointments/upcoming                   - Listar turnos agendados
 * - POST   /api/practitioner/appointments/{id}/complete              - Marcar como completado (RF09)
 * - POST   /api/practitioner/appointments/{id}/no-show               - Marcar como ausente   (RF09)
 * - POST   /api/practitioner/appointments/{id}/cancel                - Cancelar turno         (RF14)
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/practitioner")
@PreAuthorize("hasRole('PRACTITIONER')")
@Tag(name = "Practicantes", description = "Operaciones disponibles para usuarios con rol PRACTITIONER: " +
        "administrar el catálogo personal de tratamientos ofrecidos y gestionar la agenda de turnos asignados.")
@SecurityRequirement(name = "Bearer Authentication")
public class PractitionerController {

    private final IOfferedTreatmentUseCase offeredTreatmentUseCase;
    private final IAppointmentUseCase appointmentUseCase;
    private final AuthenticationFacade authenticationFacade;

    public PractitionerController(IOfferedTreatmentUseCase offeredTreatmentUseCase,
                                  IAppointmentUseCase appointmentUseCase,
                                  AuthenticationFacade authenticationFacade) {
        this.offeredTreatmentUseCase = offeredTreatmentUseCase;
        this.appointmentUseCase = appointmentUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    // ---------------------------------------------------------------------
    //  CATÁLOGO PERSONAL DEL PRACTICANTE (offered-treatments)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Agregar un tratamiento al catálogo personal (CU-005)",
            description = "Publica un tratamiento del catálogo maestro dentro del catálogo personal del " +
                    "practicante autenticado, con sus horarios de disponibilidad, duración, vigencia y " +
                    "cupo académico máximo. El practicante se infiere del JWT; el frontend no debe " +
                    "enviar `practitionerId`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Tratamiento agregado exitosamente al catálogo personal.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OfferedTreatmentResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Tratamiento creado",
                                    value = """
                                            {
                                              "id": 1,
                                              "practitionerId": 1,
                                              "practitionerName": "Maria Gomez",
                                              "treatment": {
                                                "id": 1,
                                                "name": "Limpieza completa",
                                                "description": "Eliminación de placa y sarro total",
                                                "area": "ORTODONCIA"
                                              },
                                              "requirements": "Traer cepillo dental propio",
                                              "durationInMinutes": 60,
                                              "availabilitySlots": [
                                                {
                                                  "dayOfWeek": "WEDNESDAY",
                                                  "startTime": "14:00:00",
                                                  "endTime": "18:00:00"
                                                },
                                                {
                                                  "dayOfWeek": "FRIDAY",
                                                  "startTime": "08:00:00",
                                                  "endTime": "12:00:00"
                                                }
                                              ],
                                              "offerStartDate": "2025-01-15",
                                              "offerEndDate": "2025-06-30",
                                              "maxCompletedAttentions": 10,
                                              "currentCompletedAttentions": 0,
                                              "currentActiveAttentions": 0,
                                              "currentCancelledAttentions": 0,
                                              "availabilityBlocked": false,
                                              "status": "ACTIVE"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean Validation falló (campos requeridos faltantes, " +
                            "duración no positiva, lista de horarios vacía, etc.).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El tratamiento maestro referenciado por `treatmentId` no existe.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Violación de regla de negocio: el tratamiento ya existe en el catálogo " +
                            "personal, los rangos de fecha son inválidos o los horarios son inconsistentes.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del tratamiento a publicar en el catálogo personal.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AddOfferedTreatmentRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Alta de oferta",
                            value = """
                                    {
                                      "treatmentId": 1,
                                      "requirements": "Traer cepillo dental propio",
                                      "durationInMinutes": 60,
                                      "availabilitySlots": [
                                        {
                                          "dayOfWeek": "MONDAY",
                                          "startTime": "08:00:00",
                                          "endTime": "12:00:00"
                                        },
                                        {
                                          "dayOfWeek": "WEDNESDAY",
                                          "startTime": "14:00:00",
                                          "endTime": "18:00:00"
                                        }
                                      ],
                                      "offerStartDate": "2025-01-15",
                                      "offerEndDate": "2025-06-30",
                                      "maxCompletedAttentions": 10
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/offered-treatments")
    public ResponseEntity<OfferedTreatmentResponseDTO> addTreatmentToCatalog(
            @Valid @RequestBody AddOfferedTreatmentRequestDTO request) {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        Set<AvailabilitySlot> availabilitySlots = AvailabilitySlotInputMapper.toDomainSet(request.getAvailabilitySlots());

        OfferedTreatment offeredTreatment = offeredTreatmentUseCase.addTreatmentToCatalog(
                practitionerId,
                request.getTreatmentId(),
                request.getRequirements(),
                request.getDurationInMinutes(),
                availabilitySlots,
                request.getOfferStartDate(),
                request.getOfferEndDate(),
                request.getMaxCompletedAttentions()
        );

        OfferedTreatmentResponseDTO response = OfferedTreatmentRestMapper.toResponse(offeredTreatment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene todos los tratamientos que ofrece el practicante autenticado,
     * enriquecidos con el progreso actual de atenciones completadas, activas
     * y canceladas. Corresponde al "Mi Catálogo Personal".
     *
     * Implementa una consulta optimizada que evita el problema N+1:
     * - Una consulta para obtener las ofertas del practicante.
     * - Una consulta agregada con GROUP BY para el progreso de todas las ofertas.
     * - Combinación en memoria para construir los DTOs enriquecidos.
     *
     * GET /api/practitioner/offered-treatments
     */
    @Operation(
            summary = "Obtener el catálogo personal del practicante",
            description = "Devuelve todas las ofertas de tratamientos publicadas por el practicante " +
                    "autenticado (incluyendo las desactivadas por Baja Lógica), enriquecidas con " +
                    "el progreso académico: atenciones completadas, activas y canceladas. " +
                    "Se calcula la bandera `availabilityBlocked` cuando `completed + active >= maxCompletedAttentions`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo personal del practicante, posiblemente vacío.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OfferedTreatmentResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/offered-treatments")
    public ResponseEntity<List<OfferedTreatmentResponseDTO>> getMyOfferedTreatments() {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();

        List<OfferedTreatment> offeredTreatments = offeredTreatmentUseCase.getMyOfferedTreatments(practitionerId);
        Map<Long, Long> completedMap = offeredTreatmentUseCase.getCompletedAttentionsProgressForPractitioner(practitionerId);
        Map<Long, Long> activeMap = offeredTreatmentUseCase.getActiveAttentionsProgressForPractitioner(practitionerId);
        Map<Long, Long> cancelledMap = offeredTreatmentUseCase.getCancelledAttentionsProgressForPractitioner(practitionerId);

        List<OfferedTreatmentResponseDTO> response = offeredTreatments.stream()
                .map(offer -> {
                    Long treatmentId = offer.getTreatment().getId();
                    int completedCount = completedMap.getOrDefault(treatmentId, 0L).intValue();
                    int activeCount = activeMap.getOrDefault(treatmentId, 0L).intValue();
                    int cancelledCount = cancelledMap.getOrDefault(treatmentId, 0L).intValue();
                    return OfferedTreatmentRestMapper.toResponse(offer, completedCount, activeCount, cancelledCount);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Modifica un tratamiento del catálogo personal del practicante.
     * Corresponde al CU-006: Modificar Tratamiento del Catálogo Personal.
     *
     * PUT /api/practitioner/offered-treatments/{id}
     */
    @Operation(
            summary = "Modificar una oferta del catálogo personal (CU-006)",
            description = "Actualiza requisitos, duración, horarios, vigencia y cupo máximo de una oferta " +
                    "perteneciente al practicante autenticado. La oferta debe existir y pertenecer al " +
                    "practicante autenticado (defensa de ownership en el caso de uso)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Oferta actualizada correctamente.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OfferedTreatmentResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean Validation falló (lista de horarios vacía, fechas faltantes, " +
                            "cupo no positivo, etc.).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "La oferta no pertenece al practicante autenticado (ownership) " +
                            "o el usuario no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Oferta no encontrada para el `id` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Violación de regla de negocio: rangos de fecha inválidos, horarios " +
                            "inconsistentes o cupo máximo menor a las atenciones ya completadas.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos a actualizar en la oferta.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateOfferedTreatmentRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Actualización de oferta",
                            value = """
                                    {
                                      "requirements": "Traer cepillo dental propio y radiografía panorámica.",
                                      "durationInMinutes": 45,
                                      "availabilitySlots": [
                                        {
                                          "dayOfWeek": "TUESDAY",
                                          "startTime": "09:00:00",
                                          "endTime": "13:00:00"
                                        }
                                      ],
                                      "offerStartDate": "2025-02-01",
                                      "offerEndDate": "2025-07-31",
                                      "maxCompletedAttentions": 12
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/offered-treatments/{id}")
    public ResponseEntity<OfferedTreatmentResponseDTO> updateOfferedTreatment(
            @Parameter(description = "ID de la oferta a modificar.", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferedTreatmentRequestDTO request) {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        Set<AvailabilitySlot> availabilitySlots = AvailabilitySlotInputMapper.toDomainSet(request.getAvailabilitySlots());

        OfferedTreatment offeredTreatment = offeredTreatmentUseCase.updateOfferedTreatment(
                practitionerId,
                id,
                request.getRequirements(),
                request.getDurationInMinutes(),
                availabilitySlots,
                request.getOfferStartDate(),
                request.getOfferEndDate(),
                request.getMaxCompletedAttentions()
        );

        OfferedTreatmentResponseDTO response = OfferedTreatmentRestMapper.toResponse(offeredTreatment);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina (o desactiva por integridad) un tratamiento del catálogo personal.
     * Corresponde al CU-007 con la política de RF16:
     *
     * - Si la oferta tiene turnos SCHEDULED a futuro o Atenciones IN_PROGRESS
     *   o cualquier Atención histórica → SOFT DELETE (active=false).
     * - Sólo si no hay ningún rastro → HARD DELETE.
     *
     * En ambos casos se devuelve {@code 200 OK} con un cuerpo que explica
     * la decisión al frontend, en lugar de un genérico {@code 204 No Content}.
     * El motivo es estrictamente de UX: la consecuencia de eliminar puede
     * ser silenciosa (la fila sigue en BD) y queremos que el practicante
     * vea ese hecho de forma explícita.
     *
     * Defensa de ownership: la verificación contra el ID derivado del JWT
     * la aplica el caso de uso; si la oferta no pertenece al practicante
     * autenticado responderá 403.
     *
     * DELETE /api/practitioner/offered-treatments/{id}
     */
    @Operation(
            summary = "Eliminar o desactivar una oferta del catálogo personal (CU-007 / RF16)",
            description = "Aplica una estrategia híbrida de eliminación según el rastro de la oferta:\n\n" +
                    "- **HARD DELETE**: si la oferta nunca tuvo turnos ni atenciones, se borra físicamente.\n" +
                    "- **SOFT DELETE (Baja Lógica)**: si existen turnos SCHEDULED a futuro, atenciones IN_PROGRESS " +
                    "o cualquier atención histórica, la oferta se marca como `active=false` para preservar " +
                    "la integridad referencial de las citas y atenciones ya otorgadas. La oferta deja de " +
                    "aparecer en el catálogo público pero el histórico se mantiene navegable.\n\n" +
                    "**Importante para el frontend**: en ambos casos la respuesta es `200 OK` (no `204 No Content`). " +
                    "El campo `outcome` del cuerpo (`HARD_DELETED` | `SOFT_DELETED`) indica cuál fue la decisión " +
                    "para que la UI muestre el feedback adecuado al practicante."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado de la operación. `outcome` indica si la oferta fue eliminada " +
                            "físicamente (HARD_DELETED) o desactivada lógicamente (SOFT_DELETED).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OfferedTreatmentDeletionResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Baja Lógica (RF16)",
                                            summary = "La oferta tenía turnos o atenciones asociadas",
                                            value = """
                                                    {
                                                      "outcome": "SOFT_DELETED",
                                                      "message": "La oferta se desactivó: existen turnos agendados a futuro. Se conserva la integridad referencial de las citas ya otorgadas. Ya no aparecerá en el catálogo público."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Baja Física",
                                            summary = "La oferta nunca fue utilizada",
                                            value = """
                                                    {
                                                      "outcome": "HARD_DELETED",
                                                      "message": "La oferta se eliminó físicamente: no tenía turnos ni atenciones asociadas."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "La oferta no pertenece al practicante autenticado (ownership) " +
                            "o el usuario no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Oferta no encontrada para el `id` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @DeleteMapping("/offered-treatments/{id}")
    public ResponseEntity<OfferedTreatmentDeletionResponseDTO> removeFromCatalog(
            @Parameter(description = "ID de la oferta a eliminar/desactivar.", example = "1", required = true)
            @PathVariable Long id) {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        OfferedTreatmentDeletionResult result = offeredTreatmentUseCase.removeFromCatalog(practitionerId, id);

        return ResponseEntity.ok(OfferedTreatmentDeletionResponseDTO.from(result));
    }

    // ---------------------------------------------------------------------
    //  GESTIÓN DE TURNOS DEL PRACTICANTE (appointments)
    // ---------------------------------------------------------------------

    /**
     * Obtiene todos los turnos agendados (SCHEDULED) del practicante autenticado.
     * Endpoint de lectura "Mis Turnos".
     *
     * GET /api/practitioner/appointments/upcoming
     *
     * @return Lista de turnos agendados del practicante
     */
    @Operation(
            summary = "Listar los próximos turnos del practicante",
            description = "Devuelve los turnos en estado `SCHEDULED` del practicante autenticado, " +
                    "ordenados cronológicamente. Cada elemento incluye fecha/hora, duración, " +
                    "tratamiento, paciente, atención asociada y motivo del turno. Útil para " +
                    "la vista 'Mi Agenda' y para decidir desde la UI sobre qué turno se " +
                    "habilitarán las acciones de completar, marcar ausente o cancelar."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de turnos próximos, posiblemente vacío.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AppointmentResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "Agenda del practicante",
                                    value = """
                                            [
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
                    description = "El usuario autenticado no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/appointments/upcoming")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyUpcomingAppointments() {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();

        List<Appointment> appointments = appointmentUseCase.getUpcomingAppointmentsForPractitioner(practitionerId);

        List<AppointmentResponseDTO> response = appointments.stream()
                .map(AppointmentRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Marcar un turno como completado (RF09)",
            description = "Registra la asistencia del paciente al turno. Sólo aplica sobre turnos en " +
                    "estado `SCHEDULED`; cualquier otro estado devuelve 422. El practicante autenticado " +
                    "debe ser el responsable de la atención asociada (verificación de ownership en el " +
                    "caso de uso), de lo contrario responde 403. Marcar como completado NO dispara el " +
                    "cierre automático de la atención, porque ya hubo trabajo clínico realizado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Turno marcado como completado correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Turno completado",
                                    value = """
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
                    description = "El practicante autenticado no es responsable de la atención del turno, " +
                            "o el usuario no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Turno no encontrado para el `appointmentId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "El turno no está en estado `SCHEDULED` (ya fue completado, cancelado, " +
                            "marcado como ausente, o el turno no tiene una atención válida asociada).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/appointments/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponseDTO> markAppointmentAsCompleted(
            @Parameter(description = "ID del turno a marcar como completado.", example = "45", required = true)
            @PathVariable Long appointmentId) {

        site.utnpf.odontolink.domain.model.User practitionerUser = authenticationFacade.getAuthenticatedUser();

        Appointment appointment = appointmentUseCase.markAppointmentAsCompleted(appointmentId, practitionerUser);

        AppointmentResponseDTO response = AppointmentRestMapper.toResponse(appointment);

        return ResponseEntity.ok(response);
    }

    /**
     * Marca un turno como "ausente" (el paciente no asistió).
     * Implementa RF09 - CU 4.1: Gestionar Asistencia al Turno.
     *
     * Tras la transición a NO_SHOW se evalúa la regla de funnel tracking:
     * si la Atención padre se queda sin trabajo clínico ni próximos turnos,
     * se cierra automáticamente como CANCELLED.
     *
     * POST /api/practitioner/appointments/{id}/no-show
     *
     * @param appointmentId ID del turno a marcar como ausente
     * @return El Appointment actualizado con estado NO_SHOW
     */
    @Operation(
            summary = "Marcar un turno como 'ausente' (RF09)",
            description = "Registra que el paciente no asistió al turno. Sólo aplica sobre turnos en estado " +
                    "`SCHEDULED`. El practicante autenticado debe ser el responsable de la atención. " +
                    "Tras la transición a `NO_SHOW`, si la atención padre queda sin trabajo clínico ni " +
                    "próximos turnos, el dominio la cierra automáticamente como `CANCELLED` (funnel tracking)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Turno marcado como ausente correctamente.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Turno con ausencia",
                                    value = """
                                            {
                                              "id": 45,
                                              "appointmentTime": "2025-11-15T10:00:00",
                                              "motive": "Control de rutina semestral.",
                                              "status": "NO_SHOW",
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
                    description = "El practicante autenticado no es responsable de la atención del turno, " +
                            "o el usuario no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Turno no encontrado para el `appointmentId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "El turno no está en estado `SCHEDULED` (ya fue completado, cancelado " +
                            "o marcado como ausente previamente).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/appointments/{appointmentId}/no-show")
    public ResponseEntity<AppointmentResponseDTO> markAppointmentAsNoShow(
            @Parameter(description = "ID del turno a marcar como ausente.", example = "45", required = true)
            @PathVariable Long appointmentId) {

        site.utnpf.odontolink.domain.model.User practitionerUser = authenticationFacade.getAuthenticatedUser();

        Appointment appointment = appointmentUseCase.markAppointmentAsNoShow(appointmentId, practitionerUser);

        AppointmentResponseDTO response = AppointmentRestMapper.toResponse(appointment);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancelar un turno (practicante, RF14)",
            description = "Cancela un turno en estado `SCHEDULED` por iniciativa del practicante.\n\n" +
                    "**Motivo obligatorio (RF14)**: a diferencia de la cancelación por paciente, el " +
                    "practicante DEBE enviar el campo `reason`. La cancelación afecta la agenda del " +
                    "paciente y consume cupo académico del estudiante, por lo que se exige justificación " +
                    "para trazabilidad académica y para mostrarla al paciente. Si el `reason` está " +
                    "ausente o vacío, la respuesta es **400 Bad Request** indicando el campo faltante.\n\n" +
                    "**Ownership**: el practicante autenticado debe ser el responsable de la atención " +
                    "asociada; en caso contrario responde **403 Forbidden**.\n\n" +
                    "**Funnel tracking**: tras cancelar, si la atención padre queda sin trabajo clínico " +
                    "ni próximos turnos, el dominio la cierra automáticamente como `CANCELLED`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Turno cancelado correctamente. La respuesta incluye `cancellationReason`.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Turno cancelado por practicante",
                                    value = """
                                            {
                                              "id": 45,
                                              "appointmentTime": "2025-11-15T10:00:00",
                                              "motive": "Control de rutina semestral.",
                                              "status": "CANCELLED",
                                              "durationInMinutes": 45,
                                              "cancellationReason": "Inasistencia justificada del practicante por examen final.",
                                              "treatmentId": 3,
                                              "treatmentName": "Limpieza Dental",
                                              "patientId": 15,
                                              "patientName": "Carlos Rodriguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martinez",
                                              "attentionId": 23
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El campo `reason` está ausente o vacío. Bean Validation lo exige " +
                            "(`@NotBlank`) porque es obligatorio para cancelaciones por practicante (RF14).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Motivo faltante",
                                    value = """
                                            {
                                              "timestamp": "2025-11-15T09:30:00",
                                              "status": 400,
                                              "error": "Validation Error",
                                              "message": "Los datos proporcionados no son válidos",
                                              "path": "/api/practitioner/appointments/45/cancel",
                                              "details": [
                                                "reason: El motivo de cancelación es obligatorio cuando la cancela el practicante"
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
                    description = "El practicante autenticado no es responsable de la atención del turno, " +
                            "o el usuario no posee el rol PRACTITIONER.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Turno no encontrado para el `appointmentId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "El turno no está en estado `SCHEDULED` y por lo tanto no puede cancelarse.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Motivo de la cancelación (obligatorio). Máximo 1000 caracteres.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CancelAppointmentByPractitionerRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Cancelación con motivo",
                            value = """
                                    {
                                      "reason": "Inasistencia justificada del practicante por examen final."
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @Parameter(description = "ID del turno a cancelar.", example = "45", required = true)
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentByPractitionerRequestDTO request) {

        site.utnpf.odontolink.domain.model.User practitionerUser = authenticationFacade.getAuthenticatedUser();

        Appointment cancelled = appointmentUseCase.cancelAppointmentByPractitioner(
                appointmentId,
                request.getReason(),
                practitionerUser
        );

        return ResponseEntity.ok(AppointmentRestMapper.toResponse(cancelled));
    }
}
