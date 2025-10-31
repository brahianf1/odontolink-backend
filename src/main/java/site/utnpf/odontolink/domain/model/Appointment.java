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

    // Constructor usado por la Attention
    protected Appointment(Attention attention, LocalDateTime time, String motive) {
        this.attention = attention;
        this.appointmentTime = time;
        this.motive = motive;
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
}