package site.utnpf.odontolink.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAiAgentConfigurationUseCase;
import site.utnpf.odontolink.application.port.in.dto.UpdateAiAgentConfigurationCommand;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.AgentSnapshot;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.AgentUpdateSpec;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
import site.utnpf.odontolink.domain.repository.GuardrailRepository;
import site.utnpf.odontolink.domain.repository.KnowledgeBaseDocumentRepository;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de aplicacion para la configuracion del agente IA (RF31, RF32).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Orquestar el ciclo de vida UNCONFIGURED -> DRAFT -> PUBLISHED.</li>
 *   <li>Aplicar los checks de gobernanza al publicar.</li>
 *   <li>Sincronizar la instruccion final con el proveedor externo.</li>
 *   <li>Persistir un snapshot inmutable por publish y registrar el evento
 *       en el audit log.</li>
 *   <li>Resolver health, preview y revert-to-draft.</li>
 * </ul>
 *
 * <p>Si {@code POST /publish} falla en el proveedor, el agregado queda en
 * DRAFT con {@code lastSyncError} y se registra
 * {@link AiAdminAuditEvent.Type#AGENT_PUBLISH_FAILED} para auditoria.
 */
@Transactional
public class AiAgentConfigurationService implements IAiAgentConfigurationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiAgentConfigurationService.class);

    private final AiAgentConfigurationRepository configRepository;
    private final GuardrailRepository guardrailRepository;
    private final AiGovernancePolicyRepository policyRepository;
    private final AiAgentConfigurationVersionRepository versionRepository;
    private final AiAdminAuditEventRepository auditRepository;
    private final KnowledgeBaseDocumentRepository kbDocumentRepository;
    private final ILlmAgentProviderPort llmProvider;
    private final AuthenticationFacade authFacade;
    /** UUID del agente pre-provisto en el dashboard del proveedor. */
    private final String providerAgentUuid;

    public AiAgentConfigurationService(AiAgentConfigurationRepository configRepository,
                                       GuardrailRepository guardrailRepository,
                                       AiGovernancePolicyRepository policyRepository,
                                       AiAgentConfigurationVersionRepository versionRepository,
                                       AiAdminAuditEventRepository auditRepository,
                                       KnowledgeBaseDocumentRepository kbDocumentRepository,
                                       ILlmAgentProviderPort llmProvider,
                                       AuthenticationFacade authFacade,
                                       String providerAgentUuid) {
        this.configRepository = configRepository;
        this.guardrailRepository = guardrailRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.auditRepository = auditRepository;
        this.kbDocumentRepository = kbDocumentRepository;
        this.llmProvider = llmProvider;
        this.authFacade = authFacade;
        this.providerAgentUuid = providerAgentUuid;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAgentConfiguration> findConfiguration() {
        return configRepository.findSingleton();
    }

    @Override
    public AiAgentConfiguration saveConfiguration(UpdateAiAgentConfigurationCommand cmd) {
        AiAgentConfiguration config = configRepository.findSingleton().orElse(null);
        if (config == null) {
            // Primera vez: crear via factory. La fila nace en DRAFT.
            config = AiAgentConfiguration.createNew(
                    cmd.displayName(), cmd.systemPromptCore(), cmd.welcomeMessage(),
                    cmd.temperature(), cmd.topP(), cmd.maxTokens(), cmd.k(),
                    cmd.retrievalMethod());
        } else {
            // Update: apply() valida y revierte a DRAFT.
            config.apply(
                    cmd.displayName(), cmd.systemPromptCore(), cmd.welcomeMessage(),
                    cmd.temperature(), cmd.topP(), cmd.maxTokens(), cmd.k(),
                    cmd.retrievalMethod());
        }
        return configRepository.save(config);
    }

    @Override
    public AiAgentConfiguration revertToDraft() {
        AiAgentConfiguration config = requireExistingConfiguration();
        config.markDraft();
        return configRepository.save(config);
    }

    @Override
    public AiAgentConfiguration publish(boolean override) {
        validateProviderAgentUuid();
        AiAgentConfiguration config = requireExistingConfiguration();
        AiGovernancePolicy policy = loadPolicyOrDefault();
        List<Guardrail> activeGuardrails = guardrailRepository.findAllActiveOrderByCreatedAtAsc();

        List<String> missing = computeMissingRequirements(config, policy, activeGuardrails);
        boolean usedOverride = false;
        if (!missing.isEmpty()) {
            if (!override) {
                throw new InvalidBusinessRuleException(
                        "No se puede publicar: faltan requisitos -> " + String.join(", ", missing),
                        AiAgentErrorCodes.AI_AGENT_CONFIG_INVALID);
            }
            if (!policy.isAllowOverride()) {
                throw new InvalidBusinessRuleException(
                        "Override solicitado pero la politica de gobernanza no lo permite. " +
                                "Habilite allowOverride=true en /governance antes de re-intentar.",
                        AiAgentErrorCodes.AI_AGENT_CONFIG_INVALID);
            }
            usedOverride = true;
        }

        String composedInstruction = config.composeInstruction(activeGuardrails);

        AgentUpdateSpec spec = new AgentUpdateSpec(
                config.getDisplayName(),
                composedInstruction,
                config.getTemperature(),
                config.getTopP(),
                config.getMaxTokens(),
                config.getK(),
                config.getRetrievalMethod()
        );

        Long actorId = resolveActorId();
        try {
            AgentSnapshot snapshot = llmProvider.updateAgent(providerAgentUuid, spec);
            config.markPublished(snapshot.id() != null ? snapshot.id() : providerAgentUuid, Instant.now());
            AiAgentConfiguration saved = configRepository.save(config);

            int nextVersion = versionRepository.findMaxVersionNumber() + 1;
            String missingCsv = missing.isEmpty() ? null : String.join(",", missing);
            String guardrailsLabelsCsv = activeGuardrails.stream()
                    .map(Guardrail::getLabel)
                    .collect(Collectors.joining(","));
            AiAgentConfigurationVersion version = new AiAgentConfigurationVersion(
                    null,
                    nextVersion,
                    saved.getDisplayName(),
                    saved.getSystemPromptCore(),
                    saved.getWelcomeMessage(),
                    saved.getTemperature(),
                    saved.getTopP(),
                    saved.getMaxTokens(),
                    saved.getK(),
                    saved.getRetrievalMethod(),
                    composedInstruction,
                    guardrailsLabelsCsv.isEmpty() ? null : guardrailsLabelsCsv,
                    actorId,
                    usedOverride,
                    missingCsv,
                    Instant.now()
            );
            versionRepository.save(version);

            auditRepository.save(AiAdminAuditEvent.of(
                    AiAdminAuditEvent.Type.AGENT_PUBLISH,
                    actorId,
                    nextVersion,
                    usedOverride,
                    "Publish v" + nextVersion
                            + (usedOverride ? " con override. Requisitos saltados: " + missingCsv : "")));

            return saved;
        } catch (LlmProviderException ex) {
            log.warn("Falla al publicar la configuracion del agente: {} ({}).",
                    ex.getMessage(), ex.getErrorCode());
            config.markPublishFailed(ex.getMessage());
            configRepository.save(config);

            auditRepository.save(AiAdminAuditEvent.of(
                    AiAdminAuditEvent.Type.AGENT_PUBLISH_FAILED,
                    actorId,
                    null,
                    usedOverride,
                    "Publish fallo en proveedor: " + ex.getMessage()));
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult preview() {
        AiAgentConfiguration config = requireExistingConfiguration();
        List<Guardrail> activeGuardrails = guardrailRepository.findAllActiveOrderByCreatedAtAsc();
        String composed = config.composeInstruction(activeGuardrails);
        List<String> labels = activeGuardrails.stream().map(Guardrail::getLabel).toList();
        return new PreviewResult(composed, labels);
    }

    @Override
    @Transactional(readOnly = true)
    public HealthResult health() {
        Optional<AiAgentConfiguration> opt = configRepository.findSingleton();
        AiAgentLifecycle lifecycle = opt.map(AiAgentConfiguration::getLifecycle)
                .orElse(AiAgentLifecycle.UNCONFIGURED);

        List<String> missing;
        if (opt.isPresent()) {
            AiGovernancePolicy policy = loadPolicyOrDefault();
            List<Guardrail> activeGuardrails = guardrailRepository.findAllActiveOrderByCreatedAtAsc();
            missing = computeMissingRequirements(opt.get(), policy, activeGuardrails);
        } else {
            missing = List.of("AI_AGENT_NOT_CONFIGURED");
        }

        boolean reachable = false;
        String providerError = null;
        if (providerAgentUuid != null && !providerAgentUuid.isBlank()) {
            try {
                llmProvider.getAgent(providerAgentUuid);
                reachable = true;
            } catch (LlmProviderException ex) {
                providerError = ex.getMessage();
            }
        } else {
            providerError = "DIGITALOCEAN_AGENT_UUID no configurado.";
        }

        return new HealthResult(lifecycle, missing, reachable, providerError);
    }

    /**
     * Calcula los requisitos faltantes para publicar segun la policy.
     * Devuelve lista vacia si todos los checks pasan.
     */
    private List<String> computeMissingRequirements(AiAgentConfiguration config,
                                                    AiGovernancePolicy policy,
                                                    List<Guardrail> activeGuardrails) {
        List<String> missing = new ArrayList<>();
        if (policy.isRequireSystemPrompt() &&
                (config.getSystemPromptCore() == null || config.getSystemPromptCore().isBlank())) {
            missing.add("REQUIRES_SYSTEM_PROMPT");
        }
        if (policy.isRequireWelcomeMessage() &&
                (config.getWelcomeMessage() == null || config.getWelcomeMessage().isBlank())) {
            missing.add("REQUIRES_WELCOME_MESSAGE");
        }
        if (policy.isRequireGuardrails()) {
            int active = activeGuardrails.size();
            if (active < policy.getMinActiveGuardrails()) {
                missing.add("REQUIRES_MIN_ACTIVE_GUARDRAILS:" + policy.getMinActiveGuardrails()
                        + ":have:" + active);
            }
        }
        if (policy.isRequireIndexedDocuments()) {
            List<KnowledgeBaseDocument> indexed = kbDocumentRepository
                    .findByStatusIn(List.of(KnowledgeBaseDocumentStatus.INDEXED));
            if (indexed.isEmpty()) {
                missing.add("REQUIRES_INDEXED_DOCUMENTS");
            }
        }
        return missing;
    }

    private AiAgentConfiguration requireExistingConfiguration() {
        return configRepository.findSingleton()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AiAgentConfiguration", "singleton", "1"));
    }

    private AiGovernancePolicy loadPolicyOrDefault() {
        // Bootstrap perezoso del singleton de governance: si el admin
        // todavia no edito la policy, persistimos la version estricta por
        // defecto para que cualquier check use valores consistentes y la
        // pantalla del admin muestre la fila editable de entrada.
        return policyRepository.findSingleton()
                .orElseGet(() -> policyRepository.save(AiGovernancePolicy.defaultStrict()));
    }

    private void validateProviderAgentUuid() {
        if (providerAgentUuid == null || providerAgentUuid.isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El modulo de IA no esta configurado: falta DIGITALOCEAN_AGENT_UUID.");
        }
    }

    private Long resolveActorId() {
        try {
            return authFacade.getAuthenticatedUser().getId();
        } catch (Exception ex) {
            // No deberia ocurrir bajo @PreAuthorize, pero defendemos.
            return null;
        }
    }
}
