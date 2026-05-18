package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_provider_guardrails} (RF31).
 *
 * <p>Espejo local de los guardrails nativos del proveedor (DigitalOcean
 * Gradient, hoy). Guarda la intencion del admin (attached + priority) que se
 * reconcilia con el proveedor en cada publish.
 *
 * <p>Indice unico sobre {@code provider_guardrail_uuid} para garantizar que
 * no se duplique el mismo recurso del proveedor en el espejo.
 */
@Entity
@Table(
        name = "ai_provider_guardrails",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_provider_guardrails_uuid",
                columnNames = "provider_guardrail_uuid"),
        indexes = {
                @Index(name = "ix_ai_provider_guardrails_attached", columnList = "attached")
        }
)
public class ProviderGuardrailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "provider_guardrail_uuid", nullable = false, length = 100, updatable = false)
    private String providerGuardrailUuid;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "attached", nullable = false)
    private boolean attached;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "default_response", columnDefinition = "TEXT")
    private String defaultResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProviderGuardrailEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderGuardrailUuid() { return providerGuardrailUuid; }
    public void setProviderGuardrailUuid(String providerGuardrailUuid) { this.providerGuardrailUuid = providerGuardrailUuid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isAttached() { return attached; }
    public void setAttached(boolean attached) { this.attached = attached; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getDefaultResponse() { return defaultResponse; }
    public void setDefaultResponse(String defaultResponse) { this.defaultResponse = defaultResponse; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
