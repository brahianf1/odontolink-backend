package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_admin_audit_events} (RF31).
 *
 * <p>Indice por {@code occurred_at DESC} para acelerar el listado mas
 * reciente primero (caso de uso principal).
 */
@Entity
@Table(name = "ai_admin_audit_events", indexes = {
        @Index(name = "ix_ai_audit_events_occurred_at", columnList = "occurred_at")
})
public class AiAdminAuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private AiAdminAuditEvent.Type type;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "related_version_number")
    private Integer relatedVersionNumber;

    @Column(name = "with_override", nullable = false)
    private boolean withOverride;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public AiAdminAuditEventEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AiAdminAuditEvent.Type getType() {
        return type;
    }

    public void setType(AiAdminAuditEvent.Type type) {
        this.type = type;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Integer getRelatedVersionNumber() {
        return relatedVersionNumber;
    }

    public void setRelatedVersionNumber(Integer relatedVersionNumber) {
        this.relatedVersionNumber = relatedVersionNumber;
    }

    public boolean isWithOverride() {
        return withOverride;
    }

    public void setWithOverride(boolean withOverride) {
        this.withOverride = withOverride;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
