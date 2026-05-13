package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.domain.repository.InstitutionalSettingsRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.time.LocalDateTime;

/**
 * Servicio de Dominio que implementa el "Rulebook" para la reserva de turnos.
 * Ejecuta las reglas de negocio críticas del modelo "intent-driven":
 *
 * <ol>
 *   <li><b>Validar la Oferta:</b> verifica que el OfferedTreatment existe.</li>
 *   <li><b>Validar Disponibilidad:</b> el horario debe caer dentro de un
 *       AvailabilitySlot publicado.</li>
 *   <li><b>Validar Conflictos:</b> ni el paciente ni el practicante deben
 *       tener un turno solapado en el tiempo.</li>
 *   <li><b>Agrupar en la Atención existente:</b> si ya hay un caso clínico
 *       IN_PROGRESS para el trío paciente/practicante/tratamiento, el nuevo
 *       turno se agrega ahí; si no, se crea una Atención nueva.</li>
 *   <li><b>Regla anti-acaparamiento dinámica:</b> respeta el límite de
 *       turnos SCHEDULED concurrentes que define
 *       {@link InstitutionalSettings#getMaxConcurrentAppointmentsPerAttention()}.
 *       El valor se lee en cada reserva para que el administrador pueda
 *       ajustarlo sin redeploy y los efectos sean inmediatos.</li>
 * </ol>
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
    private final ChatSessionRepository chatSessionRepository;
    private final InstitutionalSettingsRepository institutionalSettingsRepository;

    public AppointmentBookingService(
            OfferedTreatmentRepository offeredTreatmentRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            AppointmentRepository appointmentRepository,
            AttentionRepository attentionRepository,
            ChatSessionRepository chatSessionRepository,
            InstitutionalSettingsRepository institutionalSettingsRepository) {
        this.offeredTreatmentRepository = offeredTreatmentRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.attentionRepository = attentionRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.institutionalSettingsRepository = institutionalSettingsRepository;
    }

    /**
     * Reserva un turno aplicando todas las reglas del "Rulebook".
     *
     * Maneja tanto la <b>primera</b> reserva (crea atómicamente Atención +
     * Appointment) como las <b>subsecuentes</b> (agrupa el turno dentro de
     * la Atención IN_PROGRESS existente). El consumidor no necesita saber
     * en qué caso se encuentra: la decisión vive aquí.
     *
     * Flujo de ejecución:
     * <ol>
     *   <li>Cargar y validar el OfferedTreatment.</li>
     *   <li>Validar que el appointmentTime esté dentro de un AvailabilitySlot válido.</li>
     *   <li>Validar que no existan conflictos de horario (Paciente y Practicante).</li>
     *   <li>Buscar (o crear) la Atención IN_PROGRESS para el trío.</li>
     *   <li>Aplicar la regla anti-acaparamiento usando el límite dinámico
     *       de InstitutionalSettings sobre la Atención resultante.</li>
     *   <li>Materializar el nuevo Appointment dentro de la Atención.</li>
     *   <li>Crear ChatSession si todavía no existe (RF27).</li>
     * </ol>
     *
     * @param patient            El paciente que solicita el turno
     * @param offeredTreatmentId El ID de la oferta de tratamiento seleccionada
     * @param appointmentTime    La fecha y hora exactas del turno solicitado
     * @return La Atención (existente o nueva) con el Appointment recién agregado
     * @throws ResourceNotFoundException    Si el OfferedTreatment no existe
     * @throws InvalidBusinessRuleException Si alguna regla de negocio falla
     */
    public Attention bookAppointment(
            Patient patient,
            Long offeredTreatmentId,
            LocalDateTime appointmentTime) {

        // Validar la Oferta (Regla de Negocio)
        OfferedTreatment offeredTreatment = validateOfferedTreatment(offeredTreatmentId);

        // Obtener la duración del servicio para las validaciones
        int durationInMinutes = offeredTreatment.getDurationInMinutes();

        // Validar Disponibilidad (Regla de Negocio 1)
        validateAvailability(offeredTreatmentId, appointmentTime);

        // Validar Conflictos (Regla de Negocio 2) usando rangos de tiempo
        validateNoConflicts(patient, offeredTreatment.getPractitioner(), appointmentTime, durationInMinutes);

        // Buscar o Crear la Attention (el "Caso Clínico")
        Attention attention = findOrCreateAttention(
                patient,
                offeredTreatment.getPractitioner(),
                offeredTreatment.getTreatment()
        );

        // Regla anti-acaparamiento (Regla de Negocio 3 - límite dinámico)
        enforceConcurrentAppointmentLimit(attention);

        // Usar el método de dominio rico de Attention para crear el Appointment
        // Si la Atención ya existía, el motivo se diferencia para no inducir
        // a confusión al lector clínico (esto NO es el "primer turno").
        String motive = (attention.getId() == null)
                ? "Primer turno - Inicio de tratamiento"
                : "Turno adicional del caso";
        attention.scheduleAppointment(appointmentTime, motive, durationInMinutes);

        // Crear ChatSession automáticamente si no existe (RF27)
        // Esto establece el canal de comunicación entre paciente y practicante
        createChatSessionIfNotExists(patient, offeredTreatment.getPractitioner());

        // Devolver la Attention (con el Appointment en su lista)
        // El servicio de aplicación se encargará de la persistencia transaccional
        return attention;
    }

    /**
     * Aplica la regla anti-acaparamiento dinámica.
     *
     * Lee el límite vigente de los InstitutionalSettings —no está
     * hardcodeado— y lo compara con la cantidad de turnos SCHEDULED ya
     * vivos en la Atención. Si el nuevo turno haría que se exceda el
     * límite, se aborta la reserva.
     *
     * Casos límite contemplados:
     * <ul>
     *   <li><b>Atención nueva</b> (id null): no hay SCHEDULED previos, el
     *       conteo es 0 y +1 cabe siempre que el límite sea ≥ 1 (lo cual
     *       el dominio de Settings ya garantiza).</li>
     *   <li><b>Atención existente</b>: se cuentan los SCHEDULED actuales
     *       y se le suma el que estamos por crear.</li>
     * </ul>
     */
    private void enforceConcurrentAppointmentLimit(Attention attention) {
        int limit = institutionalSettingsRepository.findSingleton()
                .map(InstitutionalSettings::getMaxConcurrentAppointmentsPerAttention)
                .orElse(InstitutionalSettings.DEFAULT_MAX_CONCURRENT_APPOINTMENTS);

        long currentScheduled = (attention.getId() == null)
                ? 0L
                : appointmentRepository.countByAttentionIdAndStatus(
                        attention.getId(),
                        AppointmentStatus.SCHEDULED
                );

        if (currentScheduled + 1 > limit) {
            throw new InvalidBusinessRuleException(
                    "No puede reservar otro turno para este caso clínico mientras tenga " +
                    limit + " turno(s) agendado(s) pendiente(s). " +
                    "Espere a que se complete o cancele para reservar uno nuevo."
            );
        }
    }

    /**
     * Crea una ChatSession automáticamente si no existe una previa entre el paciente y el practicante.
     * Implementa RF27: El chat se crea automáticamente al establecerse la relación formal.
     *
     * @param patient El paciente
     * @param practitioner El practicante
     */
    private void createChatSessionIfNotExists(Patient patient, Practitioner practitioner) {
        if (!chatSessionRepository.existsByPatientAndPractitioner(patient, practitioner)) {
            ChatSession newChatSession = new ChatSession(patient, practitioner);
            chatSessionRepository.save(newChatSession);
        }
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
     * Valida que no existan conflictos de horario usando rangos de tiempo.
     * Regla de Negocio 2: Ni el Paciente ni el Practicante pueden tener otro turno activo
     * que se solape con el rango de tiempo del nuevo turno.
     *
     * Este método implementa la validación de colisiones por rango, fundamental para el
     * modelo de inventario dinámico. En lugar de verificar un punto exacto en el tiempo,
     * verifica si el rango [startTime, endTime) del nuevo turno se solapa con algún turno existente.
     *
     * @param patient            El paciente
     * @param practitioner       El practicante
     * @param appointmentTime    Hora de inicio del turno solicitado
     * @param durationInMinutes  Duración del servicio en minutos
     * @throws InvalidBusinessRuleException si hay conflictos
     */
    private void validateNoConflicts(Patient patient, Practitioner practitioner,
                                     LocalDateTime appointmentTime, int durationInMinutes) {
        // Calcular el rango de tiempo del turno solicitado
        LocalDateTime startTime = appointmentTime;
        LocalDateTime endTime = appointmentTime.plusMinutes(durationInMinutes);

        // Verificar conflicto para el Paciente
        // Excluimos turnos CANCELLED ya que no representan un conflicto real
        // Nota: La verificación de pacientes sigue usando punto exacto por simplicidad,
        // ya que es raro que un paciente tenga múltiples turnos solapados.
        // Se podría mejorar en el futuro si es necesario.
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

        // Verificar conflicto para el Practicante usando rango de tiempo
        // Esta es la validación clave del inventario dinámico
        boolean practitionerHasConflict = appointmentRepository.hasCollisionInTimeRange(
                practitioner.getId(),
                startTime,
                endTime,
                AppointmentStatus.CANCELLED
        );

        if (practitionerHasConflict) {
            throw new InvalidBusinessRuleException(
                    "El practicante ya tiene un turno agendado que se solapa con el horario solicitado. " +
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
