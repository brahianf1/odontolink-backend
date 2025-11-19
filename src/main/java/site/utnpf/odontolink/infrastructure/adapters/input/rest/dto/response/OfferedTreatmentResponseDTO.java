package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AvailabilitySlotDTO;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO de respuesta para un tratamiento ofrecido (catálogo personal del practicante).
 */
@Schema(description = "Información completa de un tratamiento ofrecido en el catálogo personal del practicante")
public class OfferedTreatmentResponseDTO {

    @Schema(description = "ID único del tratamiento ofrecido", example = "1")
    private Long id;

    @Schema(description = "ID del practicante que ofrece el tratamiento", example = "1")
    private Long practitionerId;

    @Schema(description = "Nombre completo del practicante", example = "Maria Gomez")
    private String practitionerName;

    @Schema(description = "Información del tratamiento maestro")
    private TreatmentResponseDTO treatment;

    @Schema(description = "Requisitos específicos del practicante para este tratamiento", example = "Traer cepillo dental propio")
    private String requirements;

    @Schema(description = "Duración estimada del tratamiento en minutos", example = "60")
    private int durationInMinutes;

    @Schema(description = "Horarios de disponibilidad del practicante")
    private Set<AvailabilitySlotDTO> availabilitySlots;

    @Schema(description = "Fecha de inicio de la oferta (límite temporal)", example = "2025-01-15")
    private LocalDate offerStartDate;

    @Schema(description = "Fecha de fin de la oferta (límite temporal)", example = "2025-06-30")
    private LocalDate offerEndDate;

    @Schema(description = "Cupo máximo de casos completados (límite de stock)", example = "10")
    private Integer maxCompletedAttentions;

    @Schema(description = "Número actual de casos completados (Meta Académica)", example = "3")
    private int currentCompletedAttentions;

    @Schema(description = "Número actual de casos activos/en progreso (Carga de Trabajo)", example = "2")
    private int currentActiveAttentions;

    @Schema(description = "Número histórico de casos cancelados (Estadística de Deserción)", example = "1")
    private int currentCancelledAttentions;

    @Schema(description = "Indica si la oferta está bloqueada por alcanzar el cupo máximo (Completed + Active >= Max)", example = "false")
    private boolean isAvailabilityBlocked;

    // Constructores
    public OfferedTreatmentResponseDTO() {
    }

    public OfferedTreatmentResponseDTO(Long id, Long practitionerId, String practitionerName,
                                       TreatmentResponseDTO treatment, String requirements,
                                       int durationInMinutes, Set<AvailabilitySlotDTO> availabilitySlots,
                                       LocalDate offerStartDate, LocalDate offerEndDate,
                                       Integer maxCompletedAttentions) {
        this.id = id;
        this.practitionerId = practitionerId;
        this.practitionerName = practitionerName;
        this.treatment = treatment;
        this.requirements = requirements;
        this.durationInMinutes = durationInMinutes;
        this.availabilitySlots = availabilitySlots;
        this.offerStartDate = offerStartDate;
        this.offerEndDate = offerEndDate;
        this.maxCompletedAttentions = maxCompletedAttentions;
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

    public int getCurrentCompletedAttentions() {
        return currentCompletedAttentions;
    }

    public void setCurrentCompletedAttentions(int currentCompletedAttentions) {
        this.currentCompletedAttentions = currentCompletedAttentions;
    }

    public int getCurrentActiveAttentions() {
        return currentActiveAttentions;
    }

    public void setCurrentActiveAttentions(int currentActiveAttentions) {
        this.currentActiveAttentions = currentActiveAttentions;
    }

    public int getCurrentCancelledAttentions() {
        return currentCancelledAttentions;
    }

    public void setCurrentCancelledAttentions(int currentCancelledAttentions) {
        this.currentCancelledAttentions = currentCancelledAttentions;
    }

    public boolean isAvailabilityBlocked() {
        return isAvailabilityBlocked;
    }

    public void setAvailabilityBlocked(boolean availabilityBlocked) {
        isAvailabilityBlocked = availabilityBlocked;
    }
}
