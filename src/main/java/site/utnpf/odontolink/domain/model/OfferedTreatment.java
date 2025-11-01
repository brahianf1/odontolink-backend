package site.utnpf.odontolink.domain.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa la OFERTA de un tratamiento específico por un practicante.
 * Esta es la entidad clave del "Catálogo Personal".
 */
public class OfferedTreatment {
    private Long id;

    /** Relación N-a-1: El practicante que ofrece esto */
    private Practitioner practitioner;

    /** Relación N-a-1: El tratamiento maestro que se ofrece */
    private Treatment treatment;

    private String requirements; // "Requisitos específicos"

    /**
     * Relación 1-a-N: Las franjas horarias estructuradas
     * para esta oferta (disponibilidad).
     */
    private Set<AvailabilitySlot> availabilitySlots;

    // Constructores
    public OfferedTreatment() {
        this.availabilitySlots = new HashSet<>();
    }

    public OfferedTreatment(Practitioner practitioner, Treatment treatment, String requirements) {
        this.practitioner = practitioner;
        this.treatment = treatment;
        this.requirements = requirements;
        this.availabilitySlots = new HashSet<>();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Set<AvailabilitySlot> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlot> availabilitySlots) {
        this.availabilitySlots = availabilitySlots;
    }

    // Método de utilidad para agregar un slot de disponibilidad
    public void addAvailabilitySlot(AvailabilitySlot slot) {
        if (this.availabilitySlots == null) {
            this.availabilitySlots = new HashSet<>();
        }
        this.availabilitySlots.add(slot);
        slot.setOfferedTreatment(this);
    }

    // Método de utilidad para remover un slot de disponibilidad
    public void removeAvailabilitySlot(AvailabilitySlot slot) {
        if (this.availabilitySlots != null) {
            this.availabilitySlots.remove(slot);
            slot.setOfferedTreatment(null);
        }
    }
}