package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AgentPolicyRuleResponseDTO;

import java.util.List;

public final class AgentPolicyRuleRestMapper {

    private AgentPolicyRuleRestMapper() {
    }

    public static AgentPolicyRuleResponseDTO toResponse(AgentPolicyRule domain) {
        if (domain == null) {
            return null;
        }
        AgentPolicyRuleResponseDTO dto = new AgentPolicyRuleResponseDTO();
        dto.setId(domain.getId());
        dto.setLabel(domain.getLabel());
        dto.setText(domain.getText());
        dto.setActive(domain.isActive());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }

    public static List<AgentPolicyRuleResponseDTO> toResponseList(List<AgentPolicyRule> domain) {
        return domain.stream().map(AgentPolicyRuleRestMapper::toResponse).toList();
    }
}
