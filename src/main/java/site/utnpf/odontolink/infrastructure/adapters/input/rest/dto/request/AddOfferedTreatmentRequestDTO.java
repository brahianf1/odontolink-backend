package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * DTO para agregar un tratamiento al catálogo personal del practicante.
 * Corresponde al CU-005.
 */
public class AddOfferedTreatmentRequestDTO {

    @NotNull(message = "El ID del tratamiento es obligatorio")
    private Long treatmentId;

    private String requirements;

    @NotEmpty(message = "Debe especificar al menos un horario de disponibilidad")
    @Valid
    private Set<AvailabilitySlotDTO> availabilitySlots;

    // Constructores
    public AddOfferedTreatmentRequestDTO() {
    }

    public AddOfferedTreatmentRequestDTO(Long treatmentId, String requirements, Set<AvailabilitySlotDTO> availabilitySlots) {
        this.treatmentId = treatmentId;
        this.requirements = requirements;
        this.availabilitySlots = availabilitySlots;
    }

    // Getters y Setters
    public Long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Long treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Set<AvailabilitySlotDTO> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlotDTO> availabilitySlots) {
        this.availabilitySlots = availabilitySlots;
    }
}
