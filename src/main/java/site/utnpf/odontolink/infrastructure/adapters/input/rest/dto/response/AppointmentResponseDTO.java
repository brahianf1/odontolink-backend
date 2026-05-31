package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.AppointmentStatus;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un turno (appointment).
 * Se usa en los endpoints de consulta de turnos.
 *
 * Contiene información esencial del turno para mostrar en el frontend:
 * - Datos del turno (fecha, hora, estado)
 * - Información del tratamiento
 * - Información básica del paciente (para el practicante)
 * - Información básica del practicante (para el paciente)
 * - Motivo de cancelación, cuando aplique
 */
@Schema(description = "Información de un turno para mostrar en la agenda del practicante o del paciente.")
public class AppointmentResponseDTO {

    @Schema(description = "Identificador único del turno.", example = "45")
    private Long id;

    @Schema(description = "Fecha y hora de inicio del turno (ISO-8601 sin zona horaria).",
            example = "2025-11-15T10:00:00")
    private LocalDateTime appointmentTime;

    @Schema(description = "Motivo informado por el paciente al reservar el turno.",
            example = "Control de rutina semestral.")
    private String motive;

    @Schema(description = "Estado actual del turno.",
            example = "SCHEDULED",
            allowableValues = {"SCHEDULED", "COMPLETED", "NO_SHOW", "CANCELLED"})
    private AppointmentStatus status;

    @Schema(description = "Duración prevista del turno en minutos, heredada de la oferta.",
            example = "45")
    private int durationInMinutes;

    /**
     * Motivo de cancelación.
     * Solo se materializa cuando el turno está CANCELLED y, en el caso del
     * paciente, puede ser null si no informó motivo.
     */
    @Schema(description = "Motivo de cancelación. Sólo presente cuando `status` es `CANCELLED`. " +
            "Obligatorio si la canceló el practicante (RF14), opcional si la canceló el paciente.",
            example = "Inasistencia justificada del practicante por examen final.",
            nullable = true)
    private String cancellationReason;

    // Información del tratamiento
    @Schema(description = "ID del tratamiento del catálogo maestro asociado al turno.", example = "3")
    private Long treatmentId;

    @Schema(description = "Nombre del tratamiento asociado al turno.", example = "Limpieza Dental")
    private String treatmentName;

    // Información del paciente (para vista del practicante)
    @Schema(description = "ID del paciente del turno.", example = "15")
    private Long patientId;

    @Schema(description = "Nombre completo del paciente del turno.", example = "Carlos Rodriguez")
    private String patientName;

    @Schema(description = "URL pública de la foto de perfil del paciente. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/15/avatar.jpg", nullable = true)
    private String patientProfilePictureUrl;

    // Información del practicante (para vista del paciente)
    @Schema(description = "ID del practicante responsable del turno.", example = "8")
    private Long practitionerId;

    @Schema(description = "Nombre completo del practicante responsable del turno.",
            example = "Ana Martinez")
    private String practitionerName;

    @Schema(description = "URL pública de la foto de perfil del practicante. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/8/avatar.jpg", nullable = true)
    private String practitionerProfilePictureUrl;

    // ID de la atención asociada (para navegación)
    @Schema(description = "ID de la atención (Attention) que agrupa este turno. Útil para navegar al " +
            "expediente clínico desde la UI.", example = "23")
    private Long attentionId;

    // Constructores
    public AppointmentResponseDTO() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getMotive() {
        return motive;
    }

    public void setMotive(String motive) {
        this.motive = motive;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Long treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
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

    public String getPractitionerProfilePictureUrl() {
        return practitionerProfilePictureUrl;
    }

    public void setPractitionerProfilePictureUrl(String practitionerProfilePictureUrl) {
        this.practitionerProfilePictureUrl = practitionerProfilePictureUrl;
    }

    public Long getAttentionId() {
        return attentionId;
    }

    public void setAttentionId(Long attentionId) {
        this.attentionId = attentionId;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(int durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
