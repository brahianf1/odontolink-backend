package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AgentPolicyRule;

import java.util.List;

/**
 * Puerto de entrada para el CRUD de reglas de comportamiento del agente IA
 * (RF31, RF32). El admin define todas las reglas desde el panel; el sistema
 * no provee textos por defecto.
 *
 * <p>Las {@link AgentPolicyRule} son texto en lenguaje natural que se
 * concatena al system prompt del agente en cada publish. No se confundir con
 * {@code IProviderGuardrailAdminUseCase}, que gestiona los guardrails nativos
 * de DigitalOcean (procesadores binarios attach/detach).
 */
public interface IAgentPolicyRuleAdminUseCase {

    List<AgentPolicyRule> listRules();

    AgentPolicyRule getRule(Long id);

    AgentPolicyRule createRule(String label, String text, boolean active);

    AgentPolicyRule updateRule(Long id, String label, String text, boolean active);

    AgentPolicyRule setRuleActive(Long id, boolean active);

    void deleteRule(Long id);
}
