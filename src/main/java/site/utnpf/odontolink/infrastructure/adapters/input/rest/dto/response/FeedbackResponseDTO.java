package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Respuesta del feedback multi-criterio sobre una atención.
 *
 * <p>El campo escalar {@code rating} fue retirado en la migración a
 * multi-criterio; las puntuaciones por criterio viven en {@link #scores}.
 * Para mostrar un valor único en la UI puede usarse el score del criterio
 * "satisfacción general" (P→Pr) o "comportamiento del paciente" (Pr→Pat),
 * según convenga al consumidor.
 */
@Schema(description = "Feedback multi-criterio sobre una atención.")
public class FeedbackResponseDTO {

    @Schema(example = "12")
    private Long id;

    @Schema(description = "Comentario libre opcional", example = "Excelente atención")
    private String comment;

    @Schema(example = "2026-05-23T14:30:00Z")
    private Instant createdAt;

    @Schema(example = "15")
    private Long submittedById;

    @Schema(example = "Carlos Rodríguez")
    private String submittedByName;

    @Schema(description = "URL pública de la foto de perfil del autor del feedback. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/15/avatar.jpg", nullable = true)
    private String submittedByProfilePictureUrl;

    @Schema(description = "Rol del usuario que envió el feedback (incluye prefijo ROLE_).",
            example = "ROLE_PATIENT",
            allowableValues = {"ROLE_PATIENT", "ROLE_PRACTITIONER"})
    private String submittedByRole;

    @Schema(example = "23")
    private Long attentionId;

    @Schema(example = "Limpieza Dental")
    private String treatmentName;

    @Schema(example = "Carlos Rodríguez")
    private String patientName;

    @Schema(description = "URL pública de la foto de perfil del paciente. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/15/avatar.jpg", nullable = true)
    private String patientProfilePictureUrl;

    @Schema(example = "Ana Martínez")
    private String practitionerName;

    @Schema(description = "URL pública de la foto de perfil del practicante. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/8/avatar.jpg", nullable = true)
    private String practitionerProfilePictureUrl;

    @Schema(description = "Scores por criterio. El set depende de la dirección del feedback.")
    private List<CriterionScoreResponseDTO> scores = Collections.emptyList();

    public FeedbackResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSubmittedByProfilePictureUrl() {
        return submittedByProfilePictureUrl;
    }

    public void setSubmittedByProfilePictureUrl(String submittedByProfilePictureUrl) {
        this.submittedByProfilePictureUrl = submittedByProfilePictureUrl;
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

    public String getPatientProfilePictureUrl() {
        return patientProfilePictureUrl;
    }

    public void setPatientProfilePictureUrl(String patientProfilePictureUrl) {
        this.patientProfilePictureUrl = patientProfilePictureUrl;
    }

    public String getPractitionerName() {
        return practitionerName;
    }

    public void setPractitionerName(String practitionerName) {
        this.practitionerName = practitionerName;
    }

    public String getPractitionerProfilePictureUrl() {
        return practitionerProfilePictureUrl;
    }

    public void setPractitionerProfilePictureUrl(String practitionerProfilePictureUrl) {
        this.practitionerProfilePictureUrl = practitionerProfilePictureUrl;
    }

    public List<CriterionScoreResponseDTO> getScores() {
        return scores;
    }

    public void setScores(List<CriterionScoreResponseDTO> scores) {
        this.scores = scores == null ? Collections.emptyList() : scores;
    }
}
