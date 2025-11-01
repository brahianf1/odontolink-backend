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

    // Constructores
    public AvailabilitySlot() {
    }

    public AvailabilitySlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OfferedTreatment getOfferedTreatment() {
        return offeredTreatment;
    }

    public void setOfferedTreatment(OfferedTreatment offeredTreatment) {
        this.offeredTreatment = offeredTreatment;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    // Método de validación
    public boolean isValid() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
}