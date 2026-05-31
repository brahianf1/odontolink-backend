package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import site.utnpf.odontolink.domain.model.Role;

import java.time.Instant;

/**
 * DTO de respuesta para una sesión de chat (item del inbox).
 * Implementa CU 6.1 + CU012 paso 9: enriquecimiento para construir la UI del inbox sin
 * queries adicionales del frontend.
 *
 * Composición de la respuesta:
 *  - Datos básicos (id, participantes, createdAt).
 *  - Metadatos del inbox: unreadCount, lastMessageAt y lastMessagePreview para badges y orden.
 *  - Estado de bloqueo (RF28): permite a la UI mostrar banners y deshabilitar el input cuando aplica.
 *
 * @author OdontoLink Team
 */
public class ChatSessionResponseDTO {

    private Long id;

    // Información del paciente participante
    private Long patientId;
    private String patientName;
    private String patientProfilePictureUrl;

    // Información del practicante participante
    private Long practitionerId;
    private String practitionerName;
    private String practitionerProfilePictureUrl;

    private Instant createdAt;

    // Metadatos del inbox (CU012)

    /**
     * Cantidad de mensajes no leídos para el usuario autenticado en esta sesión.
     * Calculado como: mensajes con readAt null y sender distinto al usuario autenticado.
     * Usado por el frontend para mostrar el badge de notificación.
     */
    private long unreadCount;

    /**
     * Timestamp del último mensaje en la sesión.
     * Null si la sesión todavía no tiene mensajes (recién creada por la primera atención).
     * Usado por el frontend para ordenar el inbox por actividad reciente.
     */
    private Instant lastMessageAt;

    /**
     * Vista previa del último mensaje, truncada a 120 caracteres para no inflar el payload
     * del inbox. Se usa al estilo WhatsApp/Telegram en la lista de conversaciones.
     */
    private String lastMessagePreview;

    // Estado de bloqueo (RF28)

    /**
     * Indica si la sesión está actualmente bloqueada.
     * Cuando true, el paciente recibe 403 al intentar enviar mensajes; el practicante
     * conserva voz para seguir documentando.
     */
    private boolean blocked;

    /**
     * ID del User que ejecutó el bloqueo activo. Null si la sesión no está bloqueada.
     * Forma parte del audit trail de RF28.
     */
    private Long blockedByUserId;

    /**
     * Rol del actor en el momento del bloqueo (ej. ROLE_PRACTITIONER).
     * Útil para que la UI distinga si bloqueó el practicante u otro rol (futuro).
     */
    private Role blockedByRole;

    /**
     * Timestamp del bloqueo activo. Null si la sesión no está bloqueada.
     */
    private Instant blockedAt;

    /**
     * Motivo declarado por el practicante al bloquear. Opcional; null si no se proporcionó.
     */
    private String blockReason;

    // Constructores
    public ChatSessionResponseDTO() {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Long getBlockedByUserId() {
        return blockedByUserId;
    }

    public void setBlockedByUserId(Long blockedByUserId) {
        this.blockedByUserId = blockedByUserId;
    }

    public Role getBlockedByRole() {
        return blockedByRole;
    }

    public void setBlockedByRole(Role blockedByRole) {
        this.blockedByRole = blockedByRole;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Instant blockedAt) {
        this.blockedAt = blockedAt;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
}
