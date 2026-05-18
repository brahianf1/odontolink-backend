package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IProviderGuardrailAdminUseCase;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.AgentSnapshot;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.ProviderGuardrailSnapshot;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.ProviderGuardrail;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicacion para gestionar guardrails nativos del proveedor
 * (RF31).
 *
 * <p>Tres operaciones principales:
 * <ul>
 *   <li>{@code listGuardrails}: lista el espejo local.</li>
 *   <li>{@code refreshFromProvider}: trae lo que el proveedor reporta y
 *       lo sincroniza con el espejo local. Solo actualiza metadata
 *       descriptiva; preserva la intencion de attach del admin.</li>
 *   <li>{@code updateAttachment}: el admin marca/desmarca un guardrail. El
 *       cambio se propaga al proveedor en el proximo {@code publish()} del
 *       agente — NO inmediato, para mantener semantica DRAFT/PUBLISHED.</li>
 * </ul>
 *
 * <p>Cualquier cambio en la intencion de attach revierte la config del
 * agente a DRAFT, igual que pasa con las AgentPolicyRule.
 */
@Transactional
public class ProviderGuardrailAdminService implements IProviderGuardrailAdminUseCase {

    private final ProviderGuardrailRepository guardrailRepository;
    private final AiAgentConfigurationRepository configRepository;
    private final ILlmAgentProviderPort llmProvider;
    private final String providerAgentUuid;

    public ProviderGuardrailAdminService(ProviderGuardrailRepository guardrailRepository,
                                         AiAgentConfigurationRepository configRepository,
                                         ILlmAgentProviderPort llmProvider,
                                         String providerAgentUuid) {
        this.guardrailRepository = guardrailRepository;
        this.configRepository = configRepository;
        this.llmProvider = llmProvider;
        this.providerAgentUuid = providerAgentUuid;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderGuardrail> listGuardrails() {
        return guardrailRepository.findAllOrderByPriorityAsc();
    }

    @Override
    public List<ProviderGuardrail> refreshFromProvider() {
        if (providerAgentUuid == null || providerAgentUuid.isBlank()) {
            throw new LlmProviderException(
                    "DIGITALOCEAN_AGENT_UUID no configurado; no se puede refrescar el catalogo.",
                    null, AiAgentErrorCodes.AI_AGENT_NOT_CONFIGURED);
        }
        AgentSnapshot snapshot = llmProvider.getAgent(providerAgentUuid);
        List<ProviderGuardrailSnapshot> remote = snapshot.guardrails();
        if (remote == null || remote.isEmpty()) {
            // El proveedor no reporta ninguno (puede pasar si el agente no
            // tiene guardrails default). Devolvemos lo local sin tocar.
            return guardrailRepository.findAllOrderByPriorityAsc();
        }
        // Por cada guardrail del proveedor: si existe localmente, refresh
        // metadata descriptiva y preserva intencion. Si no existe, agregar
        // un espejo con intencion=el estado attached que reporta DO (asi
        // arrancamos con la realidad del proveedor).
        for (ProviderGuardrailSnapshot s : remote) {
            Optional<ProviderGuardrail> existing = guardrailRepository
                    .findByProviderGuardrailUuid(s.guardrailUuid());
            if (existing.isPresent()) {
                ProviderGuardrail local = existing.get();
                local.refreshMetadataFromProvider(s.name(), s.description(), s.defaultResponse());
                guardrailRepository.save(local);
            } else {
                ProviderGuardrail fresh = ProviderGuardrail.fromProviderMetadata(
                        s.guardrailUuid(), s.type(), s.name(), s.description(), s.defaultResponse());
                // Inicializamos la intencion con lo que DO reporta para no
                // re-vincular/desvincular ciegamente en el proximo publish.
                fresh.setAttachmentIntent(s.isAttached(), s.priority());
                guardrailRepository.save(fresh);
            }
        }
        return guardrailRepository.findAllOrderByPriorityAsc();
    }

    @Override
    public ProviderGuardrail updateAttachment(Long id, boolean attached, int priority) {
        if (priority < 0) {
            throw new InvalidBusinessRuleException("La prioridad no puede ser negativa.");
        }
        ProviderGuardrail guardrail = guardrailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProviderGuardrail", "id", String.valueOf(id)));
        guardrail.setAttachmentIntent(attached, priority);
        ProviderGuardrail saved = guardrailRepository.save(guardrail);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    /**
     * Si la configuracion del agente esta PUBLISHED, la baja a DRAFT.
     * Cualquier cambio en attach/priority altera el comportamiento efectivo
     * en runtime; el admin debe confirmar la nueva combinacion via publish.
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
