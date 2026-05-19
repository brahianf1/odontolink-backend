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

    /**
     * Refresca el espejo local con lo que reporta el proveedor.
     *
     * <p><b>Semantica autoritativa</b>: la fuente de verdad de "que esta
     * attached y con que priority" es el proveedor. Si el admin tenia cambios
     * DRAFT pendientes en el espejo local que no llego a publicar, se pisan
     * con la realidad del proveedor. Es la decision correcta: cuando el admin
     * pide "refresh" lo que esta diciendo es "muestrame la verdad de DO". La
     * UI deberia advertir al admin antes de disparar refresh si la config
     * esta en DRAFT.
     *
     * <p>Si el proveedor responde sin guardrails (raro pero posible cuando el
     * agente nunca tuvo default attachments), interpretamos que ningun
     * guardrail conocido localmente esta attached y actualizamos el espejo
     * en consecuencia.
     */
    @Override
    public List<ProviderGuardrail> refreshFromProvider() {
        if (providerAgentUuid == null || providerAgentUuid.isBlank()) {
            throw new LlmProviderException(
                    "DIGITALOCEAN_AGENT_UUID no configurado; no se puede refrescar el catalogo.",
                    null, AiAgentErrorCodes.AI_AGENT_NOT_CONFIGURED);
        }
        AgentSnapshot snapshot = llmProvider.getAgent(providerAgentUuid);
        List<ProviderGuardrailSnapshot> remote = snapshot.guardrails() == null
                ? List.of() : snapshot.guardrails();

        // Indexamos lo remoto por uuid para resolver en O(1) al recorrer locales.
        java.util.Map<String, ProviderGuardrailSnapshot> remoteByUuid = new java.util.HashMap<>();
        for (ProviderGuardrailSnapshot s : remote) {
            if (s.guardrailUuid() != null) {
                remoteByUuid.put(s.guardrailUuid(), s);
            }
        }

        // Paso 1: agregar al espejo local los guardrails del proveedor que aun
        // no existen localmente, copiando metadata + intencion (attached/priority)
        // exactamente como los reporta DO.
        for (ProviderGuardrailSnapshot s : remote) {
            Optional<ProviderGuardrail> existing = guardrailRepository
                    .findByProviderGuardrailUuid(s.guardrailUuid());
            if (existing.isEmpty()) {
                ProviderGuardrail fresh = ProviderGuardrail.fromProviderMetadata(
                        s.guardrailUuid(), s.type(), s.name(), s.description(), s.defaultResponse());
                fresh.setAttachmentIntent(s.isAttached(), s.priority());
                guardrailRepository.save(fresh);
            }
        }

        // Paso 2: para cada guardrail local, alinear con lo que reporta DO.
        // - Si DO lo reporta: usar attached/priority de DO.
        // - Si DO NO lo reporta: el guardrail dejo de estar disponible para
        //   este agente; lo marcamos attached=false para reflejar la realidad.
        //   No lo borramos del espejo: preservamos historico de UI.
        for (ProviderGuardrail local : guardrailRepository.findAllOrderByPriorityAsc()) {
            ProviderGuardrailSnapshot s = remoteByUuid.get(local.getProviderGuardrailUuid());
            if (s != null) {
                local.refreshMetadataFromProvider(s.name(), s.description(), s.defaultResponse());
                local.setAttachmentIntent(s.isAttached(), s.priority());
            } else {
                // Conservamos la priority anterior por si DO lo vuelve a exponer
                // en un refresh futuro; solo apagamos el toggle attached.
                local.setAttachmentIntent(false, local.getPriority());
            }
            guardrailRepository.save(local);
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
