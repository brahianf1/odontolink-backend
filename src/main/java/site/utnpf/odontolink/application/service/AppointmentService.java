package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.*;
import site.utnpf.odontolink.domain.service.AppointmentBookingService;
import site.utnpf.odontolink.domain.service.AttentionPolicyService;
import site.utnpf.odontolink.domain.service.AvailabilityGenerationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de aplicación para la gestión de turnos (appointments).
 * Implementa el puerto de entrada IAppointmentUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio es el orquestador transaccional del modelo "intent-driven":
 * <ul>
 *   <li>Reserva con creación / agrupación de Atención (CU-008).</li>
 *   <li>Gestión de asistencia (completar / no-show).</li>
 *   <li>Cancelaciones diferenciadas (paciente / practicante).</li>
 *   <li>Funnel tracking: cierre automático de la Atención por abandono.</li>
 * </ul>
 *
 * Flujo de ejecución típico:
 * Controller -> AppointmentService (aquí) -> AppointmentBookingService / AttentionPolicyService (dominio) -> Repositories
 *
 * @Transactional asegura que toda la operación (estado del turno + posible cierre de la Atención)
 * sea atómica.
 *
 * @author OdontoLink Team
 */
@Transactional
public class AppointmentService implements IAppointmentUseCase {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AttentionRepository attentionRepository;
    private final OfferedTreatmentRepository offeredTreatmentRepository;
    private final AppointmentBookingService appointmentBookingService;
    private final AvailabilityGenerationService availabilityGenerationService;
    private final AttentionPolicyService attentionPolicyService;

    public AppointmentService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            AttentionRepository attentionRepository,
            OfferedTreatmentRepository offeredTreatmentRepository,
            AppointmentBookingService appointmentBookingService,
            AvailabilityGenerationService availabilityGenerationService,
            AttentionPolicyService attentionPolicyService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.attentionRepository = attentionRepository;
        this.offeredTreatmentRepository = offeredTreatmentRepository;
        this.appointmentBookingService = appointmentBookingService;
        this.availabilityGenerationService = availabilityGenerationService;
        this.attentionPolicyService = attentionPolicyService;
    }

    /**
     * Implementa el CU-008: "Reservar Turno".
     *
     * Orquestación:
     * 1. Carga el Patient desde el repositorio.
     * 2. Delega al AppointmentBookingService (servicio de dominio) para
     *    aplicar oferta válida, disponibilidad, conflictos, agrupación y
     *    regla anti-acaparamiento dinámica.
     * 3. Persiste la Attention (y su Appointment hijo gracias a
     *    CascadeType.ALL) de forma transaccional.
     */
    @Override
    public Attention bookAppointment(Long patientId, Long offeredTreatmentId, LocalDateTime appointmentTime) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId.toString()));

        Attention attention = appointmentBookingService.bookAppointment(
                patient,
                offeredTreatmentId,
                appointmentTime
        );

        return attentionRepository.save(attention);
    }

    /**
     * Obtiene los turnos agendados (SCHEDULED) de un paciente.
     * Usado para mostrar "Mis Turnos" en el panel del paciente.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getUpcomingAppointmentsForPatient(Long patientId) {
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new ResourceNotFoundException("Patient", "id", patientId.toString());
        }
        return appointmentRepository.findByPatientIdAndStatus(patientId, AppointmentStatus.SCHEDULED);
    }

    /**
     * Obtiene los turnos agendados (SCHEDULED) de un practicante.
     * Usado para mostrar "Mis Turnos" en el panel del practicante.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getUpcomingAppointmentsForPractitioner(Long practitionerId) {
        return appointmentRepository.findByPractitionerIdAndStatus(practitionerId, AppointmentStatus.SCHEDULED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferedTreatment> getAvailableOfferedTreatments(Long treatmentId) {
        if (treatmentId != null) {
            return offeredTreatmentRepository.findByTreatmentId(treatmentId);
        }
        return offeredTreatmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableSlots(Long offeredTreatmentId, LocalDate requestedDate) {
        OfferedTreatment offeredTreatment = offeredTreatmentRepository.findById(offeredTreatmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OfferedTreatment",
                        "id",
                        offeredTreatmentId.toString()
                ));
        return availabilityGenerationService.generateAvailableSlots(offeredTreatment, requestedDate);
    }

    @Override
    public Appointment markAppointmentAsCompleted(Long appointmentId, User practitionerUser) {
        Appointment appointment = appointmentRepository.findByIdWithAttention(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment",
                        "id",
                        appointmentId.toString()
                ));

        validatePractitionerOwnership(appointment, practitionerUser);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidBusinessRuleException("Solo se puede completar un turno 'Agendado'.");
        }

        boolean updated = appointmentRepository.updateStatus(appointmentId, AppointmentStatus.COMPLETED);
        if (!updated) {
            throw new InvalidBusinessRuleException("No se pudo actualizar el estado del turno.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        // COMPLETED no dispara el funnel: al haber trabajo clínico realizado
        // la Atención debe permanecer abierta para futuras evoluciones o el
        // cierre manual del practicante.
        return appointment;
    }

    @Override
    public Appointment markAppointmentAsNoShow(Long appointmentId, User practitionerUser) {
        Appointment appointment = appointmentRepository.findByIdWithAttention(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment",
                        "id",
                        appointmentId.toString()
                ));

        validatePractitionerOwnership(appointment, practitionerUser);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidBusinessRuleException("Solo se puede marcar como 'Ausente' un turno 'Agendado'.");
        }

        boolean updated = appointmentRepository.updateStatus(appointmentId, AppointmentStatus.NO_SHOW);
        if (!updated) {
            throw new InvalidBusinessRuleException("No se pudo actualizar el estado del turno.");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);

        // Funnel tracking: si el caso queda sin trabajo clínico ni próximos
        // turnos, se cierra como CANCELLED automáticamente.
        Long attentionId = appointment.getAttention() != null ? appointment.getAttention().getId() : null;
        attentionPolicyService.closeAttentionIfAbandoned(attentionId);

        return appointment;
    }

    @Override
    public Appointment cancelAppointmentByPatient(Long appointmentId, String reason, User patientUser) {
        Appointment appointment = appointmentRepository.findByIdWithAttention(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment",
                        "id",
                        appointmentId.toString()
                ));

        validatePatientOwnership(appointment, patientUser);

        // Delegar al POJO la validación de estado y la normalización del motivo.
        // El POJO acepta motivo nulo/vacío para el paciente.
        appointment.cancelByPatient(reason);

        boolean updated = appointmentRepository.updateStatusAndCancellationReason(
                appointmentId,
                AppointmentStatus.CANCELLED,
                appointment.getCancellationReason()
        );
        if (!updated) {
            throw new InvalidBusinessRuleException("No se pudo actualizar el estado del turno.");
        }

        Long attentionId = appointment.getAttention() != null ? appointment.getAttention().getId() : null;
        attentionPolicyService.closeAttentionIfAbandoned(attentionId);

        return appointment;
    }

    @Override
    public Appointment cancelAppointmentByPractitioner(Long appointmentId, String reason, User practitionerUser) {
        Appointment appointment = appointmentRepository.findByIdWithAttention(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment",
                        "id",
                        appointmentId.toString()
                ));

        validatePractitionerOwnership(appointment, practitionerUser);

        // El POJO valida que el motivo no esté en blanco y arroja
        // InvalidBusinessRuleException si lo está.
        appointment.cancelByPractitioner(reason);

        boolean updated = appointmentRepository.updateStatusAndCancellationReason(
                appointmentId,
                AppointmentStatus.CANCELLED,
                appointment.getCancellationReason()
        );
        if (!updated) {
            throw new InvalidBusinessRuleException("No se pudo actualizar el estado del turno.");
        }

        Long attentionId = appointment.getAttention() != null ? appointment.getAttention().getId() : null;
        attentionPolicyService.closeAttentionIfAbandoned(attentionId);

        return appointment;
    }

    /**
     * Valida que el practicante autenticado sea el dueño de la atención asociada al turno.
     */
    private void validatePractitionerOwnership(Appointment appointment, User practitionerUser) {
        Attention attention = appointment.getAttention();

        if (attention == null || attention.getPractitioner() == null) {
            throw new InvalidBusinessRuleException("El turno no tiene una atención asociada válida.");
        }

        User practitionerOwner = attention.getPractitioner().getUser();
        if (practitionerOwner == null || !practitionerOwner.getId().equals(practitionerUser.getId())) {
            throw new UnauthorizedOperationException(
                    "Solo el practicante responsable de la atención puede operar sobre este turno."
            );
        }
    }

    /**
     * Valida que el paciente autenticado sea el dueño de la atención asociada al turno.
     *
     * Defensa en profundidad: aunque el endpoint del paciente exige rol
     * PATIENT vía @PreAuthorize, esta verificación garantiza que un
     * paciente con sesión válida no pueda cancelar el turno de otro
     * paciente forzando un ID en la URL.
     */
    private void validatePatientOwnership(Appointment appointment, User patientUser) {
        Attention attention = appointment.getAttention();

        if (attention == null || attention.getPatient() == null) {
            throw new InvalidBusinessRuleException("El turno no tiene una atención asociada válida.");
        }

        User patientOwner = attention.getPatient().getUser();
        if (patientOwner == null || !patientOwner.getId().equals(patientUser.getId())) {
            throw new UnauthorizedOperationException(
                    "Solo el paciente titular de la atención puede operar sobre este turno."
            );
        }
    }
}
