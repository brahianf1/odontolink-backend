package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import jakarta.validation.Valid;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones del paciente.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - GET    /api/patient/offered-treatments         - Ver catálogo de tratamientos
 * - POST   /api/patient/appointments               - Reservar turno (CU-008)
 * - GET    /api/patient/appointments/upcoming      - Ver mis turnos agendados
 *
 * Todos los endpoints están protegidos con @PreAuthorize("hasRole('PATIENT')").
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
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
     * Reserva un nuevo turno para el paciente autenticado.
     * Corresponde al CU-008: "Reservar Turno".
     *
     * Flujo:
     * 1. El paciente selecciona un OfferedTreatment del catálogo
     * 2. El paciente elige una fecha/hora dentro de la disponibilidad publicada
     * 3. El sistema valida disponibilidad y conflictos
     * 4. El sistema crea atómicamente la Attention (caso clínico) y el Appointment (turno)
     *
     * Validaciones automáticas (en el servicio de dominio):
     * - La oferta de tratamiento existe
     * - El horario está dentro de la disponibilidad del practicante
     * - Ni el paciente ni el practicante tienen otro turno a esa hora
     *
     * POST /api/patient/appointments
     *
     * @param request DTO con offeredTreatmentId y appointmentTime
     * @return La Attention creada/actualizada con el nuevo Appointment
     */
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

