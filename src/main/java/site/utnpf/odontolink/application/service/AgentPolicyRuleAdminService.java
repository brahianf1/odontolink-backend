package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAgentPolicyRuleAdminUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.repository.AgentPolicyRuleRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;

import java.util.List;

/**
 * Servicio de aplicacion para el CRUD de reglas de comportamiento del agente
 * IA (RF31, RF32).
 *
 * <p>Cualquier modificacion (alta, baja, cambio de active) revierte la
 * configuracion vigente del agente a DRAFT, garantizando que el cambio en
 * las reglas no afecte al paciente hasta el siguiente publish explicito. Sin
 * esto, el admin podria desactivar una regla y el cambio se reflejaria en el
 * proveedor solo al siguiente save de configuracion, lo que es confuso y
 * peligroso.
 */
@Transactional
public class AgentPolicyRuleAdminService implements IAgentPolicyRuleAdminUseCase {

    private final AgentPolicyRuleRepository ruleRepository;
    private final AiAgentConfigurationRepository configRepository;

    public AgentPolicyRuleAdminService(AgentPolicyRuleRepository ruleRepository,
                                       AiAgentConfigurationRepository configRepository) {
        this.ruleRepository = ruleRepository;
        this.configRepository = configRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentPolicyRule> listRules() {
        return ruleRepository.findAllOrderByCreatedAtAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentPolicyRule getRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AgentPolicyRule", "id", String.valueOf(id)));
    }

    @Override
    public AgentPolicyRule createRule(String label, String text, boolean active) {
        AgentPolicyRule rule = AgentPolicyRule.createNew(label, text, active);
        AgentPolicyRule saved = ruleRepository.save(rule);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    @Override
    public AgentPolicyRule updateRule(Long id, String label, String text, boolean active) {
        AgentPolicyRule rule = getRule(id);
        rule.apply(label, text, active);
        AgentPolicyRule saved = ruleRepository.save(rule);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    @Override
    public AgentPolicyRule setRuleActive(Long id, boolean active) {
        AgentPolicyRule rule = getRule(id);
        rule.setActive(active);
        AgentPolicyRule saved = ruleRepository.save(rule);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    @Override
    public void deleteRule(Long id) {
        if (ruleRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("AgentPolicyRule", "id", String.valueOf(id));
        }
        ruleRepository.deleteById(id);
        markConfigurationAsDraftIfPublished();
    }

    /**
     * Si la configuracion del agente esta PUBLISHED, la baja a DRAFT.
     * Cualquier cambio en el catalogo de reglas altera el prompt final
     * efectivo y el admin debe confirmar la nueva combinacion via publish.
     */
    private void markConfigurationAsDraftIfPublished() {
        configRepository.findSingleton().ifPresent(config -> {
            if (config.getLifecycle() == AiAgentLifecycle.PUBLISHED) {
                config.markDraft();
                configRepository.save(config);
            }
        });
    }
}
