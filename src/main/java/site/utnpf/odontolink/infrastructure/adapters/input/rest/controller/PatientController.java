package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AppointmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AppointmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AppointmentRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AttentionRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.OfferedTreatmentRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones del paciente.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - GET    /api/patient/offered-treatments                      - Ver catálogo de tratamientos
 * - GET    /api/patient/offered-treatments/{id}/availability    - Ver slots disponibles (inventario dinámico)
 * - POST   /api/patient/appointments                            - Reservar turno (CU-008)
 * - GET    /api/patient/appointments/upcoming                   - Ver mis turnos agendados
 *
 * Todos los endpoints están protegidos con @PreAuthorize("hasRole('PATIENT')").
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
@Tag(name = "Pacientes", description = "Operaciones disponibles para pacientes: consultar tratamientos, reservar citas y gestionar sus turnos")
@SecurityRequirement(name = "Bearer Authentication")
public class PatientController {

    private final IAppointmentUseCase appointmentUseCase;
    private final AuthenticationFacade authenticationFacade;

    public PatientController(IAppointmentUseCase appointmentUseCase,
                            AuthenticationFacade authenticationFacade) {
        this.appointmentUseCase = appointmentUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Obtiene el catálogo público de tratamientos ofrecidos por los practicantes.
     * Permite al paciente ver el catálogo antes de reservar un turno.
     *
     * El paciente puede ver:
     * - Todos los tratamientos disponibles
     * - Los practicantes que los ofrecen
     * - La disponibilidad horaria de cada uno
     * - Los requisitos específicos
     *
     * GET /api/patient/offered-treatments
     * Query param opcional: treatmentId (para filtrar por tipo de tratamiento)
     *
     * @param treatmentId ID del tipo de tratamiento para filtrar (opcional)
     * @return Lista de tratamientos ofrecidos disponibles
     */
    @GetMapping("/offered-treatments")
    public ResponseEntity<List<OfferedTreatmentResponseDTO>> getAvailableTreatments(
            @RequestParam(required = false) Long treatmentId) {

        List<OfferedTreatment> offeredTreatments = appointmentUseCase.getAvailableOfferedTreatments(treatmentId);

        List<OfferedTreatmentResponseDTO> response = offeredTreatments.stream()
                .map(OfferedTreatmentRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene los slots de tiempo disponibles para un tratamiento ofrecido en una fecha específica.
     * Implementa el nuevo modelo de inventario dinámico.
     *
     * Este endpoint permite al paciente consultar los horarios realmente disponibles
     * antes de intentar reservar un turno. El sistema calcula en tiempo real:
     * 1. Los slots teóricos basados en la duración del servicio
     * 2. Filtra los slots que colisionan con turnos ya reservados
     * 3. Devuelve solo los slots que están disponibles para reservar
     *
     * GET /api/patient/offered-treatments/{offeredTreatmentId}/availability?date=2025-01-15
     *
     * Query Parameters:
     * - date: Fecha en formato ISO (YYYY-MM-DD)
     *
     * Response: 200 OK con lista de slots disponibles
     * Ejemplo:
     * [
     *   "2025-01-15T08:00:00",
     *   "2025-01-15T08:30:00",
     *   "2025-01-15T09:00:00",
     *   "2025-01-15T10:00:00"
     * ]
     *
     * @param offeredTreatmentId ID de la oferta de tratamiento
     * @param date Fecha para consultar disponibilidad (formato ISO: YYYY-MM-DD)
     * @return ResponseEntity con la lista de slots disponibles
     */
    @GetMapping("/offered-treatments/{offeredTreatmentId}/availability")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @PathVariable Long offeredTreatmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Delegar al caso de uso para calcular el inventario dinámico
        List<LocalDateTime> availableSlots = appointmentUseCase.getAvailableSlots(offeredTreatmentId, date);

        return ResponseEntity.ok(availableSlots);
    }

    @Operation(
            summary = "Reservar turno",
            description = "Crea una cita para el paciente autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Turno reservado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttentionResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 1,
                                              "status": "IN_PROGRESS",
                                              "startDate": "2025-11-02",
                                              "patientId": 1,
                                              "patientName": "Lucas Malla",
                                              "practitionerId": 1,
                                              "practitionerName": "Maria Gomez",
                                              "treatmentId": 1,
                                              "treatmentName": "Limpieza completa",
                                              "appointments": [
                                                {
                                                  "id": 1,
                                                  "appointmentTime": "2025-12-08T11:00:00",
                                                  "motive": "Primer turno - Inicio de tratamiento",
                                                  "status": "SCHEDULED",
                                                  "durationInMinutes": 60,
                                                  "treatmentId": 1,
                                                  "treatmentName": "Limpieza completa",
                                                  "patientId": 1,
                                                  "patientName": "Lucas Malla",
                                                  "practitionerId": 1,
                                                  "practitionerName": "Maria Gomez",
                                                  "attentionId": 1
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Horario no disponible o conflicto con otro turno",
                    content = @Content(mediaType = "application/json")
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del turno a reservar",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "offeredTreatmentId": 1,
                                      "appointmentTime": "2025-12-08T11:00:00"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/appointments")
    public ResponseEntity<AttentionResponseDTO> scheduleAppointment(
            @Valid @RequestBody AppointmentRequestDTO request) {

        // Obtener el ID del paciente autenticado
        Long patientId = authenticationFacade.getAuthenticatedPatientId();

        // Delegar al caso de uso (servicio de aplicación)
        Attention attention = appointmentUseCase.scheduleFirstAppointment(
                patientId,
                request.getOfferedTreatmentId(),
                request.getAppointmentTime()
        );

        // Convertir a DTO de respuesta
        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene todos los turnos agendados (SCHEDULED) del paciente autenticado.
     * Endpoint de lectura "Mis Turnos".
     *
     * El paciente puede ver:
     * - Fecha y hora de cada turno
     * - Estado del turno
     * - Tratamiento asociado
     * - Practicante asignado
     *
     * GET /api/patient/appointments/upcoming
     *
     * @return Lista de turnos agendados del paciente
     */
    @GetMapping("/appointments/upcoming")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyUpcomingAppointments() {

        Long patientId = authenticationFacade.getAuthenticatedPatientId();

        List<Appointment> appointments = appointmentUseCase.getUpcomingAppointmentsForPatient(patientId);

        List<AppointmentResponseDTO> response = appointments.stream()
                .map(AppointmentRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}

