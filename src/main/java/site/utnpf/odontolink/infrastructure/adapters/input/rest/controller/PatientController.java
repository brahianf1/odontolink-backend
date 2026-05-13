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
import site.utnpf.odontolink.application.port.in.ISearchOfferedTreatmentsUseCase;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AppointmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CancelAppointmentByPatientRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AppointmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AppointmentRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AttentionRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.OfferedTreatmentRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.DayOfWeek;
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
    private final ISearchOfferedTreatmentsUseCase searchOfferedTreatmentsUseCase;
    private final AuthenticationFacade authenticationFacade;

    public PatientController(IAppointmentUseCase appointmentUseCase,
                            ISearchOfferedTreatmentsUseCase searchOfferedTreatmentsUseCase,
                            AuthenticationFacade authenticationFacade) {
        this.appointmentUseCase = appointmentUseCase;
        this.searchOfferedTreatmentsUseCase = searchOfferedTreatmentsUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Catálogo público con motor de búsqueda dinámica (RF09) + paginación (UX/Performance).
     *
     * Los filtros son OPCIONALES y combinables vía AND:
     *  - keyword: busca en nombre/descripción del tratamiento y en nombre/apellido del practicante (case-insensitive).
     *  - specialty: área odontológica exacta del tratamiento (case-insensitive).
     *  - availability: día de la semana (DayOfWeek: MONDAY..SUNDAY) sobre el que la oferta publica disponibilidad.
     *
     * Paginación:
     *  - page: 0-based, default 0.
     *  - size: default {@link PageQuery#DEFAULT_PAGE_SIZE}, máximo {@link PageQuery#MAX_PAGE_SIZE}.
     *  - sortBy: alias permitido (treatmentName | specialty | duration | offerStartDate | offerEndDate | id).
     *  - sortDirection: ASC (default) | DESC.
     *
     * Sólo se retornan ofertas {@code active=true}: las bajas lógicas de RF16
     * no se exponen al paciente bajo ninguna combinación de filtros.
     *
     * GET /api/patient/offered-treatments
     */
    @Operation(
            summary = "Buscar catálogo público de tratamientos (RF09)",
            description = "Motor de búsqueda dinámica y paginada del catálogo público. " +
                    "Todos los filtros son opcionales y se combinan con AND. " +
                    "Sólo se retornan ofertas activas — las bajas lógicas de RF16 no se exponen."
    )
    @GetMapping("/offered-treatments")
    public ResponseEntity<PageResponseDTO<OfferedTreatmentResponseDTO>> searchAvailableTreatments(
            @Parameter(description = "Texto libre para coincidir en nombre/descripción de tratamiento o nombre/apellido del practicante")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Área/Especialidad del tratamiento (ej. ORTODONCIA)")
            @RequestParam(required = false) String specialty,
            @Parameter(description = "Día de la semana en el que la oferta tiene disponibilidad publicada (MONDAY..SUNDAY)")
            @RequestParam(required = false) DayOfWeek availability,
            @Parameter(description = "Número de página (0-based)")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "Campo de ordenamiento (treatmentName | specialty | duration | offerStartDate | offerEndDate | id)")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Dirección de ordenamiento (ASC | DESC)")
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection) {

        OfferedTreatmentSearchCriteria criteria =
                new OfferedTreatmentSearchCriteria(keyword, specialty, availability);
        PageQuery pageQuery = PageQuery.of(page, size, sortBy, sortDirection);

        PageResult<OfferedTreatment> result =
                searchOfferedTreatmentsUseCase.search(criteria, pageQuery);

        PageResponseDTO<OfferedTreatmentResponseDTO> response =
                PageResponseDTO.of(result, OfferedTreatmentRestMapper::toResponse);

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

        // Delegar al caso de uso (servicio de aplicación).
        // El caso de uso decide si crea una Atención nueva o agrupa el turno
        // dentro de una IN_PROGRESS existente, y aplica la regla
        // anti-acaparamiento usando el límite dinámico de InstitutionalSettings.
        Attention attention = appointmentUseCase.bookAppointment(
                patientId,
                request.getOfferedTreatmentId(),
                request.getAppointmentTime()
        );

        // Convertir a DTO de respuesta
        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Cancelar turno (paciente)",
            description = "Cancela un turno SCHEDULED por iniciativa del paciente. " +
                    "El motivo es opcional; si la Atención padre queda sin turnos efectivos " +
                    "ni próximos se cierra automáticamente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Turno cancelado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "El paciente no es titular del turno",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "Turno no encontrado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "422",
                    description = "El turno no está en estado SCHEDULED",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody(required = false) CancelAppointmentByPatientRequestDTO request) {

        site.utnpf.odontolink.domain.model.User patientUser = authenticationFacade.getAuthenticatedUser();
        // El motivo es opcional: si el body viene vacío o sin reason, se pasa null
        // y el dominio normaliza la ausencia de texto sin error.
        String reason = (request != null) ? request.getReason() : null;

        Appointment cancelled = appointmentUseCase.cancelAppointmentByPatient(
                appointmentId,
                reason,
                patientUser
        );

        return ResponseEntity.ok(AppointmentRestMapper.toResponse(cancelled));
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

