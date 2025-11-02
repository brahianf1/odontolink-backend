package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * DTO para respuesta de sesión de chat.
 * Implementa CU 6.1: Obtener Lista de Sesiones de Chat (El "Inbox").
 *
 * Contiene información básica de la sesión para mostrar en la lista de chats.
 * Incluye nombres de los participantes para facilitar la visualización en el frontend.
 *
 * @author OdontoLink Team
 */
public class ChatSessionResponseDTO {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long practitionerId;
    private String practitionerName;
    private Instant createdAt;

    // Constructor sin argumentos
    public ChatSessionResponseDTO() {
    }

    public ChatSessionResponseDTO(Long id, Long patientId, String patientName,
                                  Long practitionerId, String practitionerName, Instant createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.practitionerId = practitionerId;
        this.practitionerName = practitionerName;
        this.createdAt = createdAt;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
