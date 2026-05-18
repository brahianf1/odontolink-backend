package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_guardrails} (RF31, RF32).
 *
 * <p><b>Nota sobre el nombre de la tabla</b>: la clase se renombro de
 * {@code GuardrailEntity} a {@code AgentPolicyRuleEntity} para reflejar
 * que esto NO son guardrails en el sentido de DigitalOcean (procesadores
 * binarios), sino <strong>politicas de comportamiento</strong> que se
 * concatenan al system prompt. El nombre fisico de la tabla se mantiene
 * como {@code ai_guardrails} por compatibilidad con el schema existente —
 * renombrar requiere una migracion Flyway/Liquibase que se hara en un PR
 * dedicado cuando se introduzca el sistema de migraciones al proyecto.
 *
 * <p>Indice sobre {@code active} para acelerar la consulta del
 * {@code composeInstruction()} que carga solo las activas.
 */
@Entity
@Table(name = "ai_guardrails", indexes = {
        @Index(name = "ix_ai_guardrails_active", columnList = "active")
})
public class AgentPolicyRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AgentPolicyRuleEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
