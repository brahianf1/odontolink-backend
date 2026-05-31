package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.AttentionStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para una atención (caso clínico).
 * Se usa como respuesta al crear un turno, ya que la operación crea/actualiza una Attention.
 *
 * Contiene la información completa de la atención y sus turnos asociados.
 */
@Schema(description = "Caso clínico (atención) completo: estado, paciente, practicante, tratamiento y " +
        "lista de turnos asociados. Es el objeto base del expediente clínico que consume el frontend.")
public class AttentionResponseDTO {

    @Schema(description = "Identificador único del caso clínico.", example = "23")
    private Long id;

    @Schema(description = "Estado del caso. `IN_PROGRESS` permite registrar evoluciones y agendar/cancelar " +
            "turnos; `COMPLETED` es el cierre exitoso por el practicante y habilita el flujo de feedback; " +
            "`CANCELLED` es el cierre lógico por abandono temprano (sin trabajo clínico realizado).",
            example = "IN_PROGRESS",
            allowableValues = {"IN_PROGRESS", "COMPLETED", "CANCELLED"})
    private AttentionStatus status;

    @Schema(description = "Fecha de inicio del caso (creación). Formato ISO-8601 sin zona horaria.",
            example = "2025-11-10")
    private LocalDate startDate;

    // Información del paciente
    @Schema(description = "ID del paciente del caso.", example = "15")
    private Long patientId;

    @Schema(description = "Nombre completo del paciente del caso.", example = "Carlos Rodriguez")
    private String patientName;

    @Schema(description = "URL pública de la foto de perfil del paciente. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/15/avatar.jpg", nullable = true)
    private String patientProfilePictureUrl;

    // Información del practicante
    @Schema(description = "ID del practicante responsable del caso.", example = "8")
    private Long practitionerId;

    @Schema(description = "Nombre completo del practicante responsable del caso.", example = "Ana Martinez")
    private String practitionerName;

    @Schema(description = "URL pública de la foto de perfil del practicante. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/8/avatar.jpg", nullable = true)
    private String practitionerProfilePictureUrl;

    // Información del tratamiento
    @Schema(description = "ID del tratamiento del catálogo maestro asociado al caso.", example = "3")
    private Long treatmentId;

    @Schema(description = "Nombre del tratamiento asociado al caso.", example = "Limpieza Dental")
    private String treatmentName;

    // Lista de turnos asociados a esta atención
    @Schema(description = "Lista de turnos (appointments) que componen el caso clínico, en cualquier estado " +
            "(SCHEDULED, COMPLETED, NO_SHOW, CANCELLED).")
    private List<AppointmentResponseDTO> appointments;

    // Constructores
    public AttentionResponseDTO() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttentionStatus getStatus() {
        return status;
    }

    public void setStatus(AttentionStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
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

    public List<AppointmentResponseDTO> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<AppointmentResponseDTO> appointments) {
        this.appointments = appointments;
    }
}
