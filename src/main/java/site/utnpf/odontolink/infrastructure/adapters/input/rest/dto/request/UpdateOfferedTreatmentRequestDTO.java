package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO para modificar un tratamiento del catálogo personal del practicante.
 * Corresponde al CU-006.
 *
 * Todos los campos son obligatorios en la actualización para garantizar
 * que las ofertas siempre tengan límites claros definidos.
 */
@Schema(description = "Datos requeridos para modificar un tratamiento del catálogo personal del practicante")
public class UpdateOfferedTreatmentRequestDTO {

    @Schema(description = "Requisitos específicos del practicante para este tratamiento", example = "Traer cepillo dental propio")
    private String requirements;

    @Schema(description = "Duración estimada del tratamiento en minutos", example = "60")
    @Positive(message = "La duración debe ser un número positivo")
    private Integer durationInMinutes;

    @Schema(description = "Horarios de disponibilidad del practicante para este tratamiento", required = true)
    @NotEmpty(message = "Debe especificar al menos un horario de disponibilidad")
    @Valid
    private Set<AvailabilitySlotDTO> availabilitySlots;

    @Schema(description = "Fecha de inicio de la oferta (límite temporal)", example = "2025-01-15", required = true)
    @NotNull(message = "La fecha de inicio de la oferta es obligatoria")
    private LocalDate offerStartDate;

    @Schema(description = "Fecha de fin de la oferta (límite temporal)", example = "2025-06-30", required = true)
    @NotNull(message = "La fecha de fin de la oferta es obligatoria")
    private LocalDate offerEndDate;

    @Schema(description = "Cupo máximo de casos completados (límite de stock)", example = "10", required = true)
    @NotNull(message = "El cupo máximo de casos completados es obligatorio")
    @Positive(message = "El cupo debe ser un número positivo")
    private Integer maxCompletedAttentions;

    // Constructores
    public UpdateOfferedTreatmentRequestDTO() {
    }

    public UpdateOfferedTreatmentRequestDTO(String requirements, Integer durationInMinutes,
                                            Set<AvailabilitySlotDTO> availabilitySlots, LocalDate offerStartDate,
                                            LocalDate offerEndDate, Integer maxCompletedAttentions) {
        this.requirements = requirements;
        this.durationInMinutes = durationInMinutes;
        this.availabilitySlots = availabilitySlots;
        this.offerStartDate = offerStartDate;
        this.offerEndDate = offerEndDate;
        this.maxCompletedAttentions = maxCompletedAttentions;
    }

    // Getters y Setters
    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Integer getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(Integer durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public Set<AvailabilitySlotDTO> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlotDTO> availabilitySlots) {
        this.availabilitySlots = availabilitySlots;
    }

    public LocalDate getOfferStartDate() {
        return offerStartDate;
    }

    public void setOfferStartDate(LocalDate offerStartDate) {
        this.offerStartDate = offerStartDate;
    }

    public LocalDate getOfferEndDate() {
        return offerEndDate;
    }

    public void setOfferEndDate(LocalDate offerEndDate) {
        this.offerEndDate = offerEndDate;
    }

    public Integer getMaxCompletedAttentions() {
        return maxCompletedAttentions;
    }

    public void setMaxCompletedAttentions(Integer maxCompletedAttentions) {
        this.maxCompletedAttentions = maxCompletedAttentions;
    }
}
