package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Día no laborable (feriado nacional, receso institucional, etc.)")
public class NonWorkingDayResponseDTO {

    @Schema(description = "Fecha del día no laborable.", example = "2026-05-25")
    private LocalDate date;

    @Schema(description = "Origen del día no laborable.", example = "NATIONAL_HOLIDAY")
    private String source;

    @Schema(description = "Nombre descriptivo.", example = "Día de la Revolución de Mayo")
    private String name;

    @Schema(description = "Subtipo (solo para feriados nacionales: inamovible, trasladable, puente).",
            example = "inamovible")
    private String type;

    public NonWorkingDayResponseDTO() {
    }

    public NonWorkingDayResponseDTO(LocalDate date, String source, String name, String type) {
        this.date = date;
        this.source = source;
        this.name = name;
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
