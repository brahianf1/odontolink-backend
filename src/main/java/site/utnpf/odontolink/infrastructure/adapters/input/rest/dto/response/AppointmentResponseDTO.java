package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

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
 */
public class AppointmentResponseDTO {

    private Long id;
    private LocalDateTime appointmentTime;
    private String motive;
    private AppointmentStatus status;

    // Información del tratamiento
    private Long treatmentId;
    private String treatmentName;

    // Información del paciente (para vista del practicante)
    private Long patientId;
    private String patientName;

    // Información del practicante (para vista del paciente)
    private Long practitionerId;
    private String practitionerName;

    // ID de la atención asociada (para navegación)
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

    public Long getAttentionId() {
        return attentionId;
    }

    public void setAttentionId(Long attentionId) {
        this.attentionId = attentionId;
    }
}
