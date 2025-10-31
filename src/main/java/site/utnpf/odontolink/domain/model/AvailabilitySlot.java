package site.utnpf.odontolink.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Representa una única franja horaria de disponibilidad
 * para un OfferedTreatment, como "Lunes de 9:00 a 12:00".
 */
public class AvailabilitySlot {
    private Long id;

    /** Relación N-a-1: La oferta a la que pertenece esta franja */
    private OfferedTreatment offeredTreatment;

    private DayOfWeek dayOfWeek; // (ej. DayOfWeek.MONDAY)
    private LocalTime startTime; // (ej. 09:00)
    private LocalTime endTime;   // (ej. 12:00)
}