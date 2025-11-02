package site.utnpf.odontolink.domain.model;

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

    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("No se puede cancelar un turno ya completado.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    public void markAsNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
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
}