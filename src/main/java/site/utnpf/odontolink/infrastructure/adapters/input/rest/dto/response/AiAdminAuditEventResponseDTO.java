package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiAdminAuditEventResponseDTO {

    private Long id;
    private AiAdminAuditEvent.Type type;
    private Long actorUserId;
    private Integer relatedVersionNumber;
    private boolean withOverride;
    private String details;
    private Instant occurredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AiAdminAuditEvent.Type getType() { return type; }
    public void setType(AiAdminAuditEvent.Type type) { this.type = type; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public Integer getRelatedVersionNumber() { return relatedVersionNumber; }
    public void setRelatedVersionNumber(Integer relatedVersionNumber) { this.relatedVersionNumber = relatedVersionNumber; }

    public boolean isWithOverride() { return withOverride; }
    public void setWithOverride(boolean withOverride) { this.withOverride = withOverride; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
