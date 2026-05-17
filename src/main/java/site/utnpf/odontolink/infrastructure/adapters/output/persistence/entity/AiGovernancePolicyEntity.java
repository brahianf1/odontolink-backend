package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_governance_policy} (RF31).
 *
 * <p>Singleton: id fijo, sin auto-generacion (mismo patron que
 * {@code institutional_settings}).
 */
@Entity
@Table(name = "ai_governance_policy")
public class AiGovernancePolicyEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "require_guardrails", nullable = false)
    private boolean requireGuardrails;

    @Column(name = "min_active_guardrails", nullable = false)
    private int minActiveGuardrails;

    @Column(name = "require_system_prompt", nullable = false)
    private boolean requireSystemPrompt;

    @Column(name = "require_welcome_message", nullable = false)
    private boolean requireWelcomeMessage;

    @Column(name = "require_indexed_documents", nullable = false)
    private boolean requireIndexedDocuments;

    @Column(name = "allow_override", nullable = false)
    private boolean allowOverride;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiGovernancePolicyEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isRequireGuardrails() {
        return requireGuardrails;
    }

    public void setRequireGuardrails(boolean requireGuardrails) {
        this.requireGuardrails = requireGuardrails;
    }

    public int getMinActiveGuardrails() {
        return minActiveGuardrails;
    }

    public void setMinActiveGuardrails(int minActiveGuardrails) {
        this.minActiveGuardrails = minActiveGuardrails;
    }

    public boolean isRequireSystemPrompt() {
        return requireSystemPrompt;
    }

    public void setRequireSystemPrompt(boolean requireSystemPrompt) {
        this.requireSystemPrompt = requireSystemPrompt;
    }

    public boolean isRequireWelcomeMessage() {
        return requireWelcomeMessage;
    }

    public void setRequireWelcomeMessage(boolean requireWelcomeMessage) {
        this.requireWelcomeMessage = requireWelcomeMessage;
    }

    public boolean isRequireIndexedDocuments() {
        return requireIndexedDocuments;
    }

    public void setRequireIndexedDocuments(boolean requireIndexedDocuments) {
        this.requireIndexedDocuments = requireIndexedDocuments;
    }

    public boolean isAllowOverride() {
        return allowOverride;
    }

    public void setAllowOverride(boolean allowOverride) {
        this.allowOverride = allowOverride;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
