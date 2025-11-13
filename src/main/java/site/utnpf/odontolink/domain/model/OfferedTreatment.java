package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa la OFERTA de un tratamiento específico por un practicante.
 * Esta es la entidad clave del "Catálogo Personal".
 *
 * Modelo de Ofertas Finitas:
 * Las ofertas de practicantes (estudiantes) tienen límites claros:
 * - Límite temporal: fecha de inicio y fin de la oferta
 * - Límite de cupo: cantidad máxima de casos completados
 * La oferta deja de estar disponible cuando se cumple cualquiera de estos límites.
 */
public class OfferedTreatment {
    private Long id;

    /** Relación N-a-1: El practicante que ofrece esto */
    private Practitioner practitioner;

    /** Relación N-a-1: El tratamiento maestro que se ofrece */
    private Treatment treatment;

    private String requirements;

    /**
     * La duración en minutos que este practicante
     * asigna a ESTE tratamiento específico.
     * Este valor es fundamental para el cálculo del inventario dinámico de turnos.
     */
    private int durationInMinutes;

    /**
     * Relación 1-a-N: Las franjas horarias estructuradas
     * para esta oferta (disponibilidad).
     */
    private Set<AvailabilitySlot> availabilitySlots;

    /**
     * Fecha de inicio de la oferta.
     * Los turnos solo pueden agendarse desde esta fecha en adelante.
     */
    private LocalDate offerStartDate;

    /**
     * Fecha de fin de la oferta.
     * Los turnos solo pueden agendarse hasta esta fecha.
     */
    private LocalDate offerEndDate;

    /**
     * Cupo máximo de casos (Attentions) completados.
     * Cuando se alcanza este número de Attentions en estado COMPLETED,
     * la oferta deja de estar disponible.
     * Null significa sin límite de cupo.
     */
    private Integer maxCompletedAttentions;

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

    /**
     * Constructor con validación de invariantes del dominio.
     * Las reglas de negocio se aplican en el momento de la construcción.
     *
     * Todos los parámetros relacionados con límites de la oferta son obligatorios:
     * - offerStartDate: Define desde cuándo la oferta está vigente
     * - offerEndDate: Define hasta cuándo la oferta está vigente
     * - maxCompletedAttentions: Define el cupo máximo de casos que el practicante puede completar
     *
     * Esto garantiza que todas las ofertas de practicantes (estudiantes) tengan límites claros
     * definidos según las necesidades académicas.
     *
     * @param practitioner El practicante que ofrece el tratamiento
     * @param treatment El tratamiento que se ofrece
     * @param availabilitySlots Los horarios de disponibilidad
     * @param durationInMinutes Duración del tratamiento en minutos
     * @param offerStartDate Fecha de inicio de la oferta (obligatoria)
     * @param offerEndDate Fecha de fin de la oferta (obligatoria)
     * @param maxCompletedAttentions Cupo máximo de casos completados (obligatorio)
     * @throws InvalidBusinessRuleException si alguna regla de negocio se viola
     */
    public OfferedTreatment(
            Practitioner practitioner,
            Treatment treatment,
            Set<AvailabilitySlot> availabilitySlots,
            int durationInMinutes,
            LocalDate offerStartDate,
            LocalDate offerEndDate,
            Integer maxCompletedAttentions) {

        validateRequiredFields(offerStartDate, offerEndDate, maxCompletedAttentions);
        validateDateRange(offerStartDate, offerEndDate);
        validateStartDateNotInPast(offerStartDate);
        validateMaxCompletedAttentions(maxCompletedAttentions);
        validateDurationInMinutes(durationInMinutes);

        this.practitioner = practitioner;
        this.treatment = treatment;
        this.availabilitySlots = availabilitySlots != null ? new HashSet<>(availabilitySlots) : new HashSet<>();
        this.durationInMinutes = durationInMinutes;
        this.offerStartDate = offerStartDate;
        this.offerEndDate = offerEndDate;
        this.maxCompletedAttentions = maxCompletedAttentions;
    }

    /**
     * Valida que los campos obligatorios no sean nulos.
     * Todas las ofertas deben tener límites temporales y de cupo definidos.
     */
    private void validateRequiredFields(LocalDate startDate, LocalDate endDate, Integer maxAttentions) {
        if (startDate == null) {
            throw new InvalidBusinessRuleException("La fecha de inicio de la oferta es obligatoria.");
        }
        if (endDate == null) {
            throw new InvalidBusinessRuleException("La fecha de fin de la oferta es obligatoria.");
        }
        if (maxAttentions == null) {
            throw new InvalidBusinessRuleException("El cupo máximo de casos completados es obligatorio.");
        }
    }

    /**
     * Valida que el rango de fechas sea coherente.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidBusinessRuleException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    /**
     * Valida que la fecha de inicio no esté en el pasado.
     */
    private void validateStartDateNotInPast(LocalDate startDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidBusinessRuleException("La fecha de inicio no puede estar en el pasado.");
        }
    }

    /**
     * Valida que el cupo máximo sea un número positivo.
     */
    private void validateMaxCompletedAttentions(Integer maxCompletedAttentions) {
        if (maxCompletedAttentions <= 0) {
            throw new InvalidBusinessRuleException("El cupo debe ser un número positivo.");
        }
    }

    /**
     * Valida que la duración sea un número positivo.
     */
    private void validateDurationInMinutes(int durationInMinutes) {
        if (durationInMinutes <= 0) {
            throw new InvalidBusinessRuleException("La duración debe ser un número positivo.");
        }
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

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public Set<AvailabilitySlot> getAvailabilitySlots() {
        return availabilitySlots;
    }

    public void setAvailabilitySlots(Set<AvailabilitySlot> availabilitySlots) {
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

    // Métodos de utilidad

    public void addAvailabilitySlot(AvailabilitySlot slot) {
        if (this.availabilitySlots == null) {
            this.availabilitySlots = new HashSet<>();
        }
        this.availabilitySlots.add(slot);
        slot.setOfferedTreatment(this);
    }

    public void removeAvailabilitySlot(AvailabilitySlot slot) {
        if (this.availabilitySlots != null) {
            this.availabilitySlots.remove(slot);
            slot.setOfferedTreatment(null);
        }
    }
}