package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * DTO para modificar un tratamiento del catálogo personal del practicante.
 * Corresponde al CU-006.
 */
public class UpdateOfferedTreatmentRequestDTO {

    private String requirements;

    @NotEmpty(message = "Debe especificar al menos un horario de disponibilidad")
    @Valid
    private Set<AvailabilitySlotDTO> availabilitySlots;

    // Constructores
    public UpdateOfferedTreatmentRequestDTO() {
    }

    public UpdateOfferedTreatmentRequestDTO(String requirements, Set<AvailabilitySlotDTO> availabilitySlots) {
        this.requirements = requirements;
        this.availabilitySlots = availabilitySlots;
    }

    // Getters y Setters
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
