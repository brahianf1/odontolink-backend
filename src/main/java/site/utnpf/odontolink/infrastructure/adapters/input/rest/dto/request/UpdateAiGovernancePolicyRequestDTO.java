package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.Min;

/**
 * Payload del PUT a {@code /api/admin/ai-agent/governance}.
 */
public class UpdateAiGovernancePolicyRequestDTO {

    private boolean requireGuardrails;

    @Min(0)
    private int minActiveGuardrails;

    private boolean requireSystemPrompt;

    private boolean requireWelcomeMessage;

    private boolean requireIndexedDocuments;

    private boolean allowOverride;

    public boolean isRequireGuardrails() { return requireGuardrails; }
    public void setRequireGuardrails(boolean requireGuardrails) { this.requireGuardrails = requireGuardrails; }

    public int getMinActiveGuardrails() { return minActiveGuardrails; }
    public void setMinActiveGuardrails(int minActiveGuardrails) { this.minActiveGuardrails = minActiveGuardrails; }

    public boolean isRequireSystemPrompt() { return requireSystemPrompt; }
    public void setRequireSystemPrompt(boolean requireSystemPrompt) { this.requireSystemPrompt = requireSystemPrompt; }

    public boolean isRequireWelcomeMessage() { return requireWelcomeMessage; }
    public void setRequireWelcomeMessage(boolean requireWelcomeMessage) { this.requireWelcomeMessage = requireWelcomeMessage; }

    public boolean isRequireIndexedDocuments() { return requireIndexedDocuments; }
    public void setRequireIndexedDocuments(boolean requireIndexedDocuments) { this.requireIndexedDocuments = requireIndexedDocuments; }

    public boolean isAllowOverride() { return allowOverride; }
    public void setAllowOverride(boolean allowOverride) { this.allowOverride = allowOverride; }
}
