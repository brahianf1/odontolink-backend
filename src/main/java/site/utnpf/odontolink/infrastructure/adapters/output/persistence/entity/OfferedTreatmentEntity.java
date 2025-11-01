package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para la tabla 'offered_treatments'.
 * Representa la oferta de un tratamiento por un practicante (catálogo personal).
 */
@Entity
@Table(name = "offered_treatments")
public class OfferedTreatmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerEntity practitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_id", nullable = false)
    private TreatmentEntity treatment;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    /**
     * Relación OneToMany con AvailabilitySlotEntity.
     * CascadeType.ALL: Al guardar/actualizar/eliminar un OfferedTreatment, se aplica en cascada a los slots.
     * orphanRemoval = true: Si un slot se remueve de la colección, se elimina de la BD.
     */
    @OneToMany(mappedBy = "offeredTreatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AvailabilitySlotEntity> availabilitySlots = new HashSet<>();

    // Constructores
    public OfferedTreatmentEntity() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PractitionerEntity getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(PractitionerEntity practitioner) {
        this.practitioner = practitioner;
    }

    public TreatmentEntity getTreatment() {
        return treatment;
    }

    public void setTreatment(TreatmentEntity treatment) {
        this.treatment = treatment;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Set<AvailabilitySlotEntity> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlotEntity> availabilitySlots) {
        this.availabilitySlots = availabilitySlots;
    }

    // Métodos de utilidad para mantener la consistencia bidireccional
    public void addAvailabilitySlot(AvailabilitySlotEntity slot) {
        this.availabilitySlots.add(slot);
        slot.setOfferedTreatment(this);
    }

    public void removeAvailabilitySlot(AvailabilitySlotEntity slot) {
        this.availabilitySlots.remove(slot);
        slot.setOfferedTreatment(null);
    }
}
