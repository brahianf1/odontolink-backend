package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

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

    /**
     * Agrega un tratamiento al catálogo personal del practicante.
     * Corresponde al CU-005: Agregar Tratamiento al Catálogo Personal.
     *
     * POST /api/practitioner/offered-treatments
     */
    @PostMapping("/offered-treatments")
    public ResponseEntity<OfferedTreatmentResponseDTO> addTreatmentToCatalog(
            @Valid @RequestBody AddOfferedTreatmentRequestDTO request) {

        Long practitionerId = authenticationFacade.getAuthenticatedPractitionerId();
        Set<AvailabilitySlot> availabilitySlots = AvailabilitySlotInputMapper.toDomainSet(request.getAvailabilitySlots());

        OfferedTreatment offeredTreatment = offeredTreatmentUseCase.addTreatmentToCatalog(
                practitionerId,
                request.getTreatmentId(),
                request.getRequirements(),
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
}
