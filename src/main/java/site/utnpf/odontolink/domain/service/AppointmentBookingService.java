package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.time.LocalDateTime;

/**
 * Servicio de Dominio que implementa el "Rulebook" para la reserva de turnos.
 * Ejecuta las reglas de negocio críticas para el CU-008: "Reservar Turno".
 *
 * Responsabilidades (El "Rulebook"):
 * 1. Validar la Oferta: Verificar que el OfferedTreatment existe
 * 2. Validar Disponibilidad: Verificar que el horario solicitado cae dentro de un AvailabilitySlot válido
 * 3. Validar Conflictos: Verificar que ni el Paciente ni el Practicante tengan otro turno a la misma hora
 * 4. Crear el Dominio: Crear la Attention (caso clínico) y el Appointment (turno) inicialmente
 *
 * Este servicio opera exclusivamente con POJOs de dominio, sin conocimiento de infraestructura.
 *
 * @author OdontoLink Team
 */
public class AppointmentBookingService {

    private final OfferedTreatmentRepository offeredTreatmentRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final AttentionRepository attentionRepository;

    public AppointmentBookingService(
            OfferedTreatmentRepository offeredTreatmentRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            AppointmentRepository appointmentRepository,
            AttentionRepository attentionRepository) {
        this.offeredTreatmentRepository = offeredTreatmentRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.attentionRepository = attentionRepository;
    }

    /**
     * Reserva el primer turno para un paciente, creando atómicamente el "Caso Clínico" (Attention)
     * y la primera "Cita" (Appointment).
     *
     * Este es el método principal que implementa toda la lógica del "Rulebook":
     * - Valida la oferta
     * - Valida la disponibilidad del horario
     * - Valida que no haya conflictos
     * - Crea la Attention y su primer Appointment
     *
     * Flujo de ejecución:
     * 1. Cargar y validar el OfferedTreatment
     * 2. Validar que el appointmentTime esté dentro de un AvailabilitySlot válido
     * 3. Validar que no existan conflictos de horario (Paciente y Practicante)
     * 4. Verificar si ya existe una Attention abierta para este paciente-practicante-tratamiento
     * 5. Si existe, agregar el turno a esa Attention; si no, crear una nueva Attention
     * 6. Devolver la Attention con el nuevo Appointment
     *
     * @param patient            El paciente que solicita el turno
     * @param offeredTreatmentId El ID de la oferta de tratamiento seleccionada
     * @param appointmentTime    La fecha y hora exactas del turno solicitado
     * @return La Attention creada o actualizada, con el nuevo Appointment en su lista
     * @throws ResourceNotFoundException    Si el OfferedTreatment no existe
     * @throws InvalidBusinessRuleException Si alguna regla de negocio falla
     */
    public Attention bookFirstAppointment(
            Patient patient,
            Long offeredTreatmentId,
            LocalDateTime appointmentTime) {

        // Validar la Oferta (Regla de Negocio)
        OfferedTreatment offeredTreatment = validateOfferedTreatment(offeredTreatmentId);

        // Validar Disponibilidad (Regla de Negocio 1)
        validateAvailability(offeredTreatmentId, appointmentTime);

        // Validar Conflictos (Regla de Negocio 2)
        validateNoConflicts(patient, offeredTreatment.getPractitioner(), appointmentTime);

        // Buscar o Crear la Attention (el "Caso Clínico")
        Attention attention = findOrCreateAttention(
                patient,
                offeredTreatment.getPractitioner(),
                offeredTreatment.getTreatment()
        );

        // Usar el método de dominio rico de Attention para crear el Appointment
        Appointment newAppointment = attention.scheduleAppointment(
                appointmentTime,
                "Primer turno - Inicio de tratamiento"
        );

        // Devolver la Attention (con el Appointment en su lista)
        // El servicio de aplicación se encargará de la persistencia transaccional
        return attention;
    }

    /**
     * Valida que la oferta de tratamiento existe y está disponible.
     *
     * @param offeredTreatmentId ID de la oferta
     * @return El OfferedTreatment cargado
     * @throws ResourceNotFoundException si no existe
     */
    private OfferedTreatment validateOfferedTreatment(Long offeredTreatmentId) {
        return offeredTreatmentRepository.findById(offeredTreatmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OfferedTreatment",
                        "id",
                        offeredTreatmentId.toString()
                ));
    }

    /**
     * Valida que el horario solicitado cae dentro de un AvailabilitySlot válido.
     * Regla de Negocio 1: El turno debe estar dentro de la disponibilidad publicada.
     *
     * @param offeredTreatmentId ID de la oferta
     * @param appointmentTime    Horario solicitado
     * @throws InvalidBusinessRuleException si el horario no es válido
     */
    private void validateAvailability(Long offeredTreatmentId, LocalDateTime appointmentTime) {
        boolean isAvailable = availabilitySlotRepository.isTimeWithinAvailability(
                offeredTreatmentId,
                appointmentTime.getDayOfWeek(),
                appointmentTime.toLocalTime()
        );

        if (!isAvailable) {
            throw new InvalidBusinessRuleException(
                    "El horario seleccionado no está disponible. " +
                            "Por favor, elija un horario dentro de la disponibilidad publicada del practicante."
            );
        }
    }

    /**
     * Valida que no existan conflictos de horario.
     * Regla de Negocio 2: Ni el Paciente ni el Practicante pueden tener otro turno activo a la misma hora.
     *
     * @param patient         El paciente
     * @param practitioner    El practicante
     * @param appointmentTime El horario solicitado
     * @throws InvalidBusinessRuleException si hay conflictos
     */
    private void validateNoConflicts(Patient patient, Practitioner practitioner, LocalDateTime appointmentTime) {
        // Verificar conflicto para el Paciente
        // Excluimos turnos CANCELLED ya que no representan un conflicto real
        boolean patientHasConflict = appointmentRepository.existsByPatientIdAndAppointmentTimeAndStatusNot(
                patient.getId(),
                appointmentTime,
                AppointmentStatus.CANCELLED
        );

        if (patientHasConflict) {
            throw new InvalidBusinessRuleException(
                    "Ya tiene un turno agendado para esta fecha y hora. " +
                            "No puede reservar dos turnos al mismo tiempo."
            );
        }

        // Verificar conflicto para el Practicante
        boolean practitionerHasConflict = appointmentRepository.existsByPractitionerIdAndAppointmentTimeAndStatusNot(
                practitioner.getId(),
                appointmentTime,
                AppointmentStatus.CANCELLED
        );

        if (practitionerHasConflict) {
            throw new InvalidBusinessRuleException(
                    "El practicante ya tiene un turno agendado para esta fecha y hora. " +
                            "Por favor, seleccione otro horario disponible."
            );
        }
    }

    /**
     * Busca una Attention existente en estado IN_PROGRESS, o crea una nueva si no existe.
     *
     * Lógica de negocio: Si un paciente ya tiene un caso abierto (IN_PROGRESS) con el mismo
     * practicante y tratamiento, se reutiliza ese caso para agendar turnos subsecuentes.
     * Si no existe, se crea un nuevo caso clínico.
     *
     * @param patient      El paciente
     * @param practitioner El practicante
     * @param treatment    El tratamiento
     * @return La Attention existente o una nueva instancia (sin persistir aún)
     */
    private Attention findOrCreateAttention(Patient patient, Practitioner practitioner, Treatment treatment) {
        // Buscar si ya existe una Attention activa para este paciente-practicante-tratamiento
        return attentionRepository.findByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
                        patient.getId(),
                        practitioner.getId(),
                        treatment.getId(),
                        AttentionStatus.IN_PROGRESS
                )
                .orElseGet(() -> {
                    // Si no existe, crear una nueva Attention (Caso Clínico)
                    return new Attention(patient, practitioner, treatment);
                });
    }
}
