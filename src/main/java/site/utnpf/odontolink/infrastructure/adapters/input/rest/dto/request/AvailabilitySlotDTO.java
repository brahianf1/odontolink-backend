package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO para representar una franja horaria de disponibilidad.
 * Usado tanto en request como en response.
 *
 * La conversión entre este DTO y el objeto de dominio AvailabilitySlot
 * se realiza mediante AvailabilitySlotInputMapper.
 *
 * @author OdontoLink Team
 */
@Schema(description = "Franja horaria de disponibilidad semanal del practicante para un tratamiento.")
public class AvailabilitySlotDTO {

    @Schema(description = "Día de la semana de la franja, en inglés (enum `java.time.DayOfWeek`).",
            example = "MONDAY",
            allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"},
            required = true)
    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Hora de inicio de la franja, en formato `HH:mm:ss`.",
            example = "08:00:00",
            required = true)
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @Schema(description = "Hora de fin de la franja, en formato `HH:mm:ss`. Debe ser posterior a `startTime`.",
            example = "12:00:00",
            required = true)
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;

    // Constructores
    public AvailabilitySlotDTO() {
    }

    public AvailabilitySlotDTO(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters y Setters
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
}
