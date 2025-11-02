package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AvailabilitySlotDTO;

import java.util.Set;

/**
 * DTO de respuesta para un tratamiento ofrecido (catálogo personal del practicante).
 */
public class OfferedTreatmentResponseDTO {

    private Long id;
    private Long practitionerId;
    private String practitionerName;
    private TreatmentResponseDTO treatment;
    private String requirements;
    private int durationInMinutes;
    private Set<AvailabilitySlotDTO> availabilitySlots;

    // Constructores
    public OfferedTreatmentResponseDTO() {
    }

    public OfferedTreatmentResponseDTO(Long id, Long practitionerId, String practitionerName,
                                       TreatmentResponseDTO treatment, String requirements,
                                       Set<AvailabilitySlotDTO> availabilitySlots) {
        this.id = id;
        this.practitionerId = practitionerId;
        this.practitionerName = practitionerName;
        this.treatment = treatment;
        this.requirements = requirements;
        this.availabilitySlots = availabilitySlots;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPractitionerId() {
        return practitionerId;
    }

    public void setPractitionerId(Long practitionerId) {
        this.practitionerId = practitionerId;
    }

    public String getPractitionerName() {
        return practitionerName;
    }

    public void setPractitionerName(String practitionerName) {
        this.practitionerName = practitionerName;
    }

    public TreatmentResponseDTO getTreatment() {
        return treatment;
    }

    public void setTreatment(TreatmentResponseDTO treatment) {
        this.treatment = treatment;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public Set<AvailabilitySlotDTO> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlotDTO> availabilitySlots) {
        this.availabilitySlots = availabilitySlots;
    }
}
