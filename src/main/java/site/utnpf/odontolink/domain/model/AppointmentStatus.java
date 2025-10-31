package site.utnpf.odontolink.domain.model;

/**
 * Enumeración de estados para un Turno (Cita).
 */
public enum AppointmentStatus {
    SCHEDULED,   // "Agendado"
    COMPLETED,   // "Completado" (Asistió)
    CANCELLED,   // "Cancelado"
    NO_SHOW      // "No asistió"
}