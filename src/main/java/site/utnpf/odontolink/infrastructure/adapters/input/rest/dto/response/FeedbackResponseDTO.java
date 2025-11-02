package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * DTO de respuesta para un feedback.
 * Se usa para mostrar el feedback de una atención a los actores autorizados.
 *
 * Contiene la información completa del feedback, incluyendo datos del usuario
 * que lo envió y la atención asociada.
 *
 * @author OdontoLink Team
 */
public class FeedbackResponseDTO {

    private Long id;
    private int rating;
    private String comment;
    private Instant createdAt;

    // Información del usuario que envió el feedback
    private Long submittedById;
    private String submittedByName;
    private String submittedByRole; // PATIENT, PRACTITIONER

    // Información de la atención asociada
    private Long attentionId;

    // Información adicional de la atención (para contexto)
    private String treatmentName;
    private String patientName;
    private String practitionerName;

    // Constructores
    public FeedbackResponseDTO() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSubmittedById() {
        return submittedById;
    }

    public void setSubmittedById(Long submittedById) {
        this.submittedById = submittedById;
    }

    public String getSubmittedByName() {
        return submittedByName;
    }

    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }

    public String getSubmittedByRole() {
        return submittedByRole;
    }

    public void setSubmittedByRole(String submittedByRole) {
        this.submittedByRole = submittedByRole;
    }

    public Long getAttentionId() {
        return attentionId;
    }

    public void setAttentionId(Long attentionId) {
        this.attentionId = attentionId;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPractitionerName() {
        return practitionerName;
    }

    public void setPractitionerName(String practitionerName) {
        this.practitionerName = practitionerName;
    }
}
