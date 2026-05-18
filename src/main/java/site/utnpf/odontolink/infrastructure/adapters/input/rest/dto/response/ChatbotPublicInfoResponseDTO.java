package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiAgentAccessMode;

/**
 * Response de {@code GET /api/chatbot/info} (RF29). Le dice al FE si puede
 * renderizar el widget de chat + texto inicial.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatbotPublicInfoResponseDTO {

    private boolean accessGranted;
    private AiAgentAccessMode accessMode;
    private String displayName;
    private String welcomeMessage;
    /** Codigo estable cuando accessGranted=false (AGENT_DISABLED, etc.). */
    private String denyReason;

    public boolean isAccessGranted() { return accessGranted; }
    public void setAccessGranted(boolean accessGranted) { this.accessGranted = accessGranted; }

    public AiAgentAccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(AiAgentAccessMode accessMode) { this.accessMode = accessMode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }

    public String getDenyReason() { return denyReason; }
    public void setDenyReason(String denyReason) { this.denyReason = denyReason; }
}
