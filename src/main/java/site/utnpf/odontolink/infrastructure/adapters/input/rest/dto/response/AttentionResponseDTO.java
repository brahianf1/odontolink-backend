package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import site.utnpf.odontolink.domain.model.AttentionStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para una atención (caso clínico).
 * Se usa como respuesta al crear un turno, ya que la operación crea/actualiza una Attention.
 *
 * Contiene la información completa de la atención y sus turnos asociados.
 */
public class AttentionResponseDTO {

    private Long id;
    private AttentionStatus status;
    private LocalDate startDate;

    // Información del paciente
    private Long patientId;
    private String patientName;

    // Información del practicante
    private Long practitionerId;
    private String practitionerName;

    // Información del tratamiento
    private Long treatmentId;
    private String treatmentName;

    // Lista de turnos asociados a esta atención
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
