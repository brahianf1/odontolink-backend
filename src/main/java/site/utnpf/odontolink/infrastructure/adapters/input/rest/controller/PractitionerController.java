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
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AddOfferedTreatmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateOfferedTreatmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AppointmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AppointmentRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AvailabilitySlotInputMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.OfferedTreatmentRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;
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
 * - DELETE /api/practitioner/offered-treatments/{id}     - Eliminar tratamiento (CU-007)
 *
 * Endpoints de Turnos:
 * - GET    /api/practitioner/appointments/upcoming       - Ver mis turnos agendados
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/practitioner")
@PreAuthorize("hasRole('PRACTITIONER')")
@Tag(name = "Practicantes", description = "Operaciones para practicantes: gestionar catálogo de tratamientos ofrecidos y consultar turnos asignados")
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

    @Operation(
            summary = "Agregar tratamiento al catálogo",
            description = "Agrega un tratamiento al catálogo personal del practicante"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Tratamiento agregado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OfferedTreatmentResponseDTO.class),
                            examples = @ExampleObject(
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
                                                },
                                                {
                                                  "dayOfWeek": "MONDAY",
                                                  "startTime": "08:00:00",
                                                  "endTime": "12:00:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o tratamiento ya existe en el catálogo",
                    content = @Content(mediaType = "application/json")
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del tratamiento a agregar",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
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
                                        },
                                        {
                                          "dayOfWeek": "FRIDAY",
                                          "startTime": "08:00:00",
                                          "endTime": "12:00:00"
                                        }
                                      ]
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
                availabilitySlots
        );

        OfferedTreatmentResponseDTO response = OfferedTreatmentRestMapper.toResponse(offeredTreatment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene todos los tratamientos que ofrece el practicante autenticado.
     * Corresponde al "Mi Catálogo Personal".
     *
     * GET /api/practitioner/offered-treatments
     */
    @GetMapping("/offered-treatments")
    public ResponseEntity<List<OfferedTreatmentResponseDTO>> getMyOfferedTreatments() {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        List<OfferedTreatment> offeredTreatments = offeredTreatmentUseCase.getMyOfferedTreatments(practitionerId);

        List<OfferedTreatmentResponseDTO> response = offeredTreatments.stream()
                .map(OfferedTreatmentRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Modifica un tratamiento del catálogo personal del practicante.
     * Corresponde al CU-006: Modificar Tratamiento del Catálogo Personal.
     *
     * PUT /api/practitioner/offered-treatments/{id}
     */
    @PutMapping("/offered-treatments/{id}")
    public ResponseEntity<OfferedTreatmentResponseDTO> updateOfferedTreatment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferedTreatmentRequestDTO request) {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        Set<AvailabilitySlot> availabilitySlots = AvailabilitySlotInputMapper.toDomainSet(request.getAvailabilitySlots());

        OfferedTreatment offeredTreatment = offeredTreatmentUseCase.updateOfferedTreatment(
                practitionerId,
                id,
                request.getRequirements(),
                request.getDurationInMinutes(),
                availabilitySlots
        );

        OfferedTreatmentResponseDTO response = OfferedTreatmentRestMapper.toResponse(offeredTreatment);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un tratamiento del catálogo personal del practicante.
     * Corresponde al CU-007: Eliminar Tratamiento del Catálogo Personal.
     *
     * DELETE /api/practitioner/offered-treatments/{id}
     */
    @DeleteMapping("/offered-treatments/{id}")
    public ResponseEntity<Void> removeFromCatalog(@PathVariable Long id) {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        offeredTreatmentUseCase.removeFromCatalog(practitionerId, id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene todos los turnos agendados (SCHEDULED) del practicante autenticado.
     * Endpoint de lectura "Mis Turnos".
     *
     * El practicante puede ver:
     * - Fecha y hora de cada turno
     * - Estado del turno
     * - Paciente asociado
     * - Tratamiento asociado
     *
     * Este endpoint permite al practicante gestionar su agenda y ver qué pacientes
     * tiene agendados para cada fecha.
     *
     * GET /api/practitioner/appointments/upcoming
     *
     * @return Lista de turnos agendados del practicante
     */
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
            summary = "Marcar turno como completado",
            description = "Registra la asistencia del paciente al turno"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Turno marcado como completado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 45,
                                              "appointmentTime": "2025-11-15T10:00:00",
                                              "status": "COMPLETED",
                                              "durationInMinutes": 45,
                                              "treatmentId": 3,
                                              "treatmentName": "Limpieza Dental",
                                              "patientId": 15,
                                              "patientName": "Carlos Rodríguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martínez",
                                              "attentionId": 23
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Turno no encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/appointments/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponseDTO> markAppointmentAsCompleted(
            @PathVariable Long appointmentId) {

        site.utnpf.odontolink.domain.model.User practitionerUser = authenticationFacade.getAuthenticatedUser();

        Appointment appointment = appointmentUseCase.markAppointmentAsCompleted(appointmentId, practitionerUser);

        AppointmentResponseDTO response = AppointmentRestMapper.toResponse(appointment);

        return ResponseEntity.ok(response);
    }

    /**
     * Marca un turno como "ausente" (el paciente no asistió).
     * Implementa RF9 - CU 4.1: Gestionar Asistencia al Turno.
     *
     * Este endpoint permite al practicante registrar que el paciente no asistió a su cita.
     * El turno debe estar en estado SCHEDULED para poder ser marcado como ausente.
     *
     * POST /api/practitioner/appointments/{id}/no-show
     *
     * @param appointmentId ID del turno a marcar como ausente
     * @return El Appointment actualizado con estado NO_SHOW
     */
    @PostMapping("/appointments/{appointmentId}/no-show")
    public ResponseEntity<AppointmentResponseDTO> markAppointmentAsNoShow(
            @PathVariable Long appointmentId) {

        site.utnpf.odontolink.domain.model.User practitionerUser = authenticationFacade.getAuthenticatedUser();

        Appointment appointment = appointmentUseCase.markAppointmentAsNoShow(appointmentId, practitionerUser);

        AppointmentResponseDTO response = AppointmentRestMapper.toResponse(appointment);

        return ResponseEntity.ok(response);
    }
}
