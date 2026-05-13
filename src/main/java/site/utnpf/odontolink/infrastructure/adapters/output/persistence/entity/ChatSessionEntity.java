package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import site.utnpf.odontolink.domain.model.Role;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla 'chat_sessions'.
 *
 * Modela RF26/RF27 (sesión paciente-practicante) y RF28 (bloqueo auditable).
 * Las columnas de bloqueo se añaden con DEFAULTs y NULLs para que la migración
 * Hibernate (ddl-auto=update) pueda aplicarse sobre filas existentes sin romper.
 *
 * @author OdontoLink Team
 */
@Entity
@Table(name = "chat_sessions",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_chat_session_patient_practitioner",
                columnNames = {"patient_id", "practitioner_id"}
            )
        },
        indexes = {
            @Index(name = "idx_chat_session_patient", columnList = "patient_id"),
            @Index(name = "idx_chat_session_practitioner", columnList = "practitioner_id"),
            @Index(name = "idx_chat_session_blocked", columnList = "is_blocked")
        })
public class ChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerEntity practitioner;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessageEntity> messages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Campos de bloqueo (RF28). Defaults en BD para soportar filas legacy en ddl-auto=update.

    @Column(name = "is_blocked", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean blocked;

    /**
     * FK al User que ejecutó el bloqueo. Nullable porque solo se completa cuando hay un bloqueo activo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_user_id", nullable = true)
    private UserEntity blockedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "blocked_by_role", length = 32, nullable = true)
    private Role blockedByRole;

    @Column(name = "blocked_at", nullable = true)
    private Instant blockedAt;

    @Column(name = "block_reason", length = 500, nullable = true)
    private String blockReason;

    public ChatSessionEntity() {
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addMessage(ChatMessageEntity message) {
        this.messages.add(message);
        message.setChatSession(this);
    }

    public void removeMessage(ChatMessageEntity message) {
        this.messages.remove(message);
        message.setChatSession(null);
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public PractitionerEntity getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(PractitionerEntity practitioner) {
        this.practitioner = practitioner;
    }

    public List<ChatMessageEntity> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageEntity> messages) {
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

    public UserEntity getBlockedByUser() {
        return blockedByUser;
    }

    public void setBlockedByUser(UserEntity blockedByUser) {
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
