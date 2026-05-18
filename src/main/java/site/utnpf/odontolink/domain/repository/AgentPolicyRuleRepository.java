package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AgentPolicyRule;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para las reglas de comportamiento del agente IA (RF31, RF32).
 *
 * <p>Las {@link AgentPolicyRule} son instrucciones de texto que se concatenan
 * al system prompt del agente en cada publish. Son distintas de los
 * {@link site.utnpf.odontolink.domain.model.ProviderGuardrail}, que son los
 * guardrails nativos del proveedor (procesadores binarios attach/detach).
 */
public interface AgentPolicyRuleRepository {

    List<AgentPolicyRule> findAllOrderByCreatedAtAsc();

    List<AgentPolicyRule> findAllActiveOrderByCreatedAtAsc();

    Optional<AgentPolicyRule> findById(Long id);

    AgentPolicyRule save(AgentPolicyRule policyRule);

    void deleteById(Long id);

    long countActive();
}
