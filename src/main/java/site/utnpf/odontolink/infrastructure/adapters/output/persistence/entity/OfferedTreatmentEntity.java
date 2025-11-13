package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
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
     * La duración en minutos que este practicante asigna a este tratamiento específico.
     * Este valor es fundamental para el cálculo del inventario dinámico de turnos.
     */
    @Column(name = "duration_in_minutes", nullable = false)
    private int durationInMinutes;

    /**
     * Relación OneToMany con AvailabilitySlotEntity.
     * CascadeType.ALL: Al guardar/actualizar/eliminar un OfferedTreatment, se aplica en cascada a los slots.
     * orphanRemoval = true: Si un slot se remueve de la colección, se elimina de la BD.
     */
    @OneToMany(mappedBy = "offeredTreatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AvailabilitySlotEntity> availabilitySlots = new HashSet<>();

    /**
     * Fecha de inicio de la oferta.
     * Los turnos solo pueden agendarse desde esta fecha en adelante.
     */
    @Column(name = "offer_start_date")
    private LocalDate offerStartDate;

    /**
     * Fecha de fin de la oferta.
     * Los turnos solo pueden agendarse hasta esta fecha.
     */
    @Column(name = "offer_end_date")
    private LocalDate offerEndDate;

    /**
     * Cupo máximo de casos (Attentions) completados.
     * Cuando se alcanza este número, la oferta deja de estar disponible.
     * Null significa sin límite de cupo.
     */
    @Column(name = "max_completed_attentions")
    private Integer maxCompletedAttentions;

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

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public Set<AvailabilitySlotEntity> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlotEntity> availabilitySlots) {
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
