package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.LocalDateTime;

/**
 * Representa un TURNO o cita en el calendario.
 * Es un evento de tiempo que PERTENECE a una "Atención" (Caso).
 */
public class Appointment {
    private Long id;

    /**
     * Relación N-a-1: La "Atención" (caso) a la que pertenece esta cita.
     * Esta es la conexión clave que discutimos.
     */
    private Attention attention;

    private LocalDateTime appointmentTime; // Fecha y hora del turno
    private String motive;                 // Motivo de la cita (ej. "Inicio", "Control")
    private AppointmentStatus status;      // "Agendado", "Cancelado", etc.

    /**
     * Duración en minutos del turno.
     * Este campo representa un "snapshot" de la duración del servicio en el momento
     * de la reserva. Se preserva inmutablemente para mantener la integridad del registro
     * histórico, independientemente de cambios posteriores en la configuración del servicio.
     *
     * Razón de diseño: Si un practicante cambia la duración de su servicio de 30 a 60 minutos,
     * los turnos ya reservados deben mantener la duración con la que fueron acordados (30 min).
     * Esto permite cálculos precisos de disponibilidad y auditoría histórica correcta.
     */
    private int durationInMinutes;

    /**
     * Motivo registrado al cancelar el turno.
     *
     * Reglas (consistentes con los métodos de cancelación):
     * - Cuando cancela el practicante, este campo es OBLIGATORIO (auditoría académica).
     * - Cuando cancela el paciente, puede ser nulo o vacío (información de cortesía).
     *
     * Se persiste como TEXT porque puede contener texto largo, y se conserva como
     * registro inmutable una vez fijado: el motivo refleja el contexto del momento
     * y editarlo posteriormente desvirtuaría el histórico.
     */
    private String cancellationReason;

    // Constructor sin argumentos (requerido por mappers de persistencia)
    public Appointment() {
    }

    // Constructor usado por la Attention
    protected Appointment(Attention attention, LocalDateTime time, String motive, int durationInMinutes) {
        this.attention = attention;
        this.appointmentTime = time;
        this.motive = motive;
        this.durationInMinutes = durationInMinutes;
        this.status = AppointmentStatus.SCHEDULED;
    }

    // Comportamientos del Dominio Rico

    /**
     * Cancela el turno por iniciativa del PACIENTE.
     *
     * Política de negocio (RF cancelaciones):
     * - El motivo es OPCIONAL: el paciente no tiene la obligación académica
     *   de justificarse para cancelar. Cualquier texto recibido se persiste
     *   tal cual para nutrir el funnel de seguimiento y, eventualmente,
     *   alimentar estadísticas de deserción.
     * - Solo es válido cancelar un turno en estado SCHEDULED. Cancelar un
     *   COMPLETED, CANCELLED o NO_SHOW es un error de uso del dominio.
     *
     * @param reason Motivo opcional informado por el paciente
     * @throws InvalidBusinessRuleException si el turno no está en estado SCHEDULED
     */
    public void cancelByPatient(String reason) {
        ensureCancellable();
        this.status = AppointmentStatus.CANCELLED;
        // Normalizamos cadenas vacías a null para no contaminar la BD con
        // strings sin información útil.
        this.cancellationReason = isBlank(reason) ? null : reason.trim();
    }

    /**
     * Cancela el turno por iniciativa del PRACTICANTE.
     *
     * Política de negocio (RF cancelaciones):
     * - El motivo es OBLIGATORIO: la cancelación desde el practicante
     *   impacta al paciente y al cupo del estudiante, por lo que se exige
     *   justificación para auditoría académica y para mostrarla al paciente.
     * - Solo es válido cancelar un turno en estado SCHEDULED.
     *
     * @param reason Motivo obligatorio informado por el practicante
     * @throws InvalidBusinessRuleException si el motivo está vacío
     *         o el turno no está en estado SCHEDULED
     */
    public void cancelByPractitioner(String reason) {
        if (isBlank(reason)) {
            throw new InvalidBusinessRuleException(
                    "El motivo de cancelación es obligatorio cuando la cancela el practicante."
            );
        }
        ensureCancellable();
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason.trim();
    }

    /**
     * Marca el turno como completado (el paciente asistió).
     * Este método implementa la lógica del RF9 - CU 4.1: Gestionar Asistencia al Turno.
     *
     * Reglas de negocio:
     * - Solo se puede completar un turno con estado SCHEDULED
     * - Esta operación es irreversible
     *
     * @throws IllegalStateException si el turno no está en estado SCHEDULED
     */
    public void complete() {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException("Solo se puede completar un turno 'Agendado'.");
        }
        this.status = AppointmentStatus.COMPLETED;
    }

    /**
     * Marca el turno como "ausente" (el paciente no asistió).
     * Este método implementa la lógica del RF9 - CU 4.1: Gestionar Asistencia al Turno.
     *
     * Reglas de negocio:
     * - Solo se puede marcar como ausente un turno con estado SCHEDULED
     *
     * @throws IllegalStateException si el turno no está en estado SCHEDULED
     */
    public void markAsNoShow() {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException("Solo se puede marcar como 'Ausente' un turno 'Agendado'.");
        }
        this.status = AppointmentStatus.NO_SHOW;
    }

    private void ensureCancellable() {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new InvalidBusinessRuleException(
                    "Solo se puede cancelar un turno en estado SCHEDULED."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Attention getAttention() {
        return attention;
    }

    public void setAttention(Attention attention) {
        this.attention = attention;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getMotive() {
        return motive;
    }

    public void setMotive(String motive) {
        this.motive = motive;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
