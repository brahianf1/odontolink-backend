package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.ProviderGuardrailType;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderGuardrailResponseDTO {

    private Long id;
    private String providerGuardrailUuid;
    private ProviderGuardrailType type;
    private String displayName;
    private String description;
    private boolean attached;
    private int priority;
    private String defaultResponse;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderGuardrailUuid() { return providerGuardrailUuid; }
    public void setProviderGuardrailUuid(String providerGuardrailUuid) { this.providerGuardrailUuid = providerGuardrailUuid; }

    public ProviderGuardrailType getType() { return type; }
    public void setType(ProviderGuardrailType type) { this.type = type; }

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
