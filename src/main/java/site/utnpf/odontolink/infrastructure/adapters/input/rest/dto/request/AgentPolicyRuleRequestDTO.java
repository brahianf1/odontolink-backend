package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para crear o actualizar un {@link site.utnpf.odontolink.domain.model.AgentPolicyRule}.
 */
public class AgentPolicyRuleRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String label;

    @NotBlank
    private String text;

    private boolean active = true;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
