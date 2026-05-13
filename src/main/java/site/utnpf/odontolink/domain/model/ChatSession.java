package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una sala de chat entre un paciente y un practicante.
 * La sesión se crea automáticamente cuando se establece la primera atención (RF27).
 *
 * Responsabilidades:
 * - Mantener la relación entre los participantes del chat
 * - Almacenar el historial de mensajes
 * - Modelar el estado de bloqueo (RF28): permite al practicante bloquear al paciente
 *   y deja rastro auditable (quién bloqueó, cuándo y por qué)
 *
 * @author OdontoLink Team
 */
public class ChatSession {
    private Long id;

    /** Relación N-a-1: El paciente en el chat */
    private Patient patient;

    /** Relación N-a-1: El practicante en el chat */
    private Practitioner practitioner;

    /** Relación 1-a-N: La lista de mensajes en esta sesión */
    private List<ChatMessage> messages;

    /** Timestamp de creación de la sesión */
    private Instant createdAt;

    // Campos de bloqueo (RF28). Defaults asegurados en el constructor sin argumentos.

    /** Indica si la sesión está bloqueada (RF28). */
    private boolean blocked;

    /**
     * Usuario que ejecutó el bloqueo. Lo modelamos como User (no Practitioner) porque
     * la auditoría se hace contra el actor, no contra su perfil-rol específico, y porque
     * a futuro un Admin podría intervenir sin necesidad de un Practitioner asociado.
     */
    private User blockedByUser;

    /** Rol del actor en el momento del bloqueo. Permite distinguir si fue Practitioner o un futuro Admin. */
    private Role blockedByRole;

    /** Momento exacto del bloqueo. Forma parte del audit trail exigido por RF28. */
    private Instant blockedAt;

    /** Motivo opcional. Hoy lo guardamos para que el supervisor pueda revisarlo en auditorías futuras. */
    private String blockReason;

    public ChatSession() {
        this.messages = new ArrayList<>();
        this.createdAt = Instant.now();
        this.blocked = false;
    }

    public ChatSession(Patient patient, Practitioner practitioner) {
        this();
        this.patient = patient;
        this.practitioner = practitioner;
    }

    // Comportamientos del Dominio Rico

    /**
     * Añade un mensaje a la sesión manteniendo consistencia bidireccional.
     */
    public void addMessage(ChatMessage message) {
        if (message != null) {
            this.messages.add(message);
            message.setChatSession(this);
        }
    }

    /**
     * Bloquea la sesión registrando quién y cuándo (RF28).
     *
     * Reglas:
     * - No se puede re-bloquear una sesión ya bloqueada (idempotencia explícita: no consideramos
     *   silenciosamente "ya bloqueado", lanzamos error para que el caller corrija el flujo).
     * - El motivo es opcional pero, si se envía, se persiste para auditoría.
     */
    public void block(User blocker, Role blockerRole, String reason) {
        if (this.blocked) {
            throw new InvalidBusinessRuleException("La sesión de chat ya se encuentra bloqueada.");
        }
        if (blocker == null || blockerRole == null) {
            throw new IllegalArgumentException("El usuario y el rol que ejecutan el bloqueo son obligatorios.");
        }
        this.blocked = true;
        this.blockedByUser = blocker;
        this.blockedByRole = blockerRole;
        this.blockedAt = Instant.now();
        this.blockReason = reason;
    }

    /**
     * Desbloquea la sesión y limpia el rastro de bloqueo (RF28 reversible).
     */
    public void unblock() {
        if (!this.blocked) {
            throw new InvalidBusinessRuleException("La sesión de chat no está bloqueada.");
        }
        this.blocked = false;
        this.blockedByUser = null;
        this.blockedByRole = null;
        this.blockedAt = null;
        this.blockReason = null;
    }

    /**
     * Verifica si el paciente puede continuar enviando mensajes. Si la sesión fue bloqueada
     * por el practicante, el paciente queda silenciado pero el practicante conserva voz para
     * seguir documentando el caso (decisión clínica documentada en RF28).
     */
    public void ensureSenderCanWrite(User sender) {
        if (!this.blocked) {
            return;
        }
        boolean senderIsPatient = patient != null
                && patient.getUser() != null
                && patient.getUser().getId().equals(sender.getId());
        if (senderIsPatient) {
            throw new UnauthorizedOperationException("El paciente ha sido bloqueado en esta sesión de chat por el practicante.");
        }
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public User getBlockedByUser() {
        return blockedByUser;
    }

    public void setBlockedByUser(User blockedByUser) {
        this.blockedByUser = blockedByUser;
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
