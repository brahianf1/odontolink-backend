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
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;
import site.utnpf.odontolink.domain.repository.AgentPolicyRuleRepository;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
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
    private final AgentPolicyRuleRepository policyRuleRepository;
    private final site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository providerGuardrailRepository;
    private final AiGovernancePolicyRepository policyRepository;
    private final AiAgentConfigurationVersionRepository versionRepository;
    private final AiAdminAuditEventRepository auditRepository;
    private final KnowledgeBaseDocumentRepository kbDocumentRepository;
    private final ILlmAgentProviderPort llmProvider;
    private final site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort invokerPort;
    private final AuthenticationFacade authFacade;
    private final site.utnpf.odontolink.application.service.support.SingletonRowBootstrap singletonBootstrap;
    /** UUID del agente pre-provisto en el dashboard del proveedor. */
    private final String providerAgentUuid;
    /** ENV {@code DIGITALOCEAN_AGENT_INVOCATION_URL}; gana sobre el cache de BD. */
    private final String envAgentInvocationUrl;

    public AiAgentConfigurationService(AiAgentConfigurationRepository configRepository,
                                       AgentPolicyRuleRepository policyRuleRepository,
                                       site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository providerGuardrailRepository,
                                       AiGovernancePolicyRepository policyRepository,
                                       AiAgentConfigurationVersionRepository versionRepository,
                                       AiAdminAuditEventRepository auditRepository,
                                       KnowledgeBaseDocumentRepository kbDocumentRepository,
                                       ILlmAgentProviderPort llmProvider,
                                       site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort invokerPort,
                                       AuthenticationFacade authFacade,
                                       site.utnpf.odontolink.application.service.support.SingletonRowBootstrap singletonBootstrap,
                                       String providerAgentUuid,
                                       String envAgentInvocationUrl) {
        this.configRepository = configRepository;
        this.policyRuleRepository = policyRuleRepository;
        this.providerGuardrailRepository = providerGuardrailRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.auditRepository = auditRepository;
        this.kbDocumentRepository = kbDocumentRepository;
        this.llmProvider = llmProvider;
        this.invokerPort = invokerPort;
        this.authFacade = authFacade;
        this.singletonBootstrap = singletonBootstrap;
        this.providerAgentUuid = providerAgentUuid;
        this.envAgentInvocationUrl = envAgentInvocationUrl;
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
            // Primera vez: crear via factory. La fila nace en DRAFT con los
            // defaults conservadores del chatbot (DISABLED, BLOCK, etc.) y
            // luego aplicamos el bloque de chatbot si el admin lo envio en el
            // mismo PUT. La separacion en dos apply() mantiene cada subdominio
            // (agente IA core vs chatbot institucional) con su propia
            // validacion granular.
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
        // Bloque del chatbot institucional. Si el admin no envio estos campos
        // (caller legado), los validadores del dominio rechazan con 422 y se
        // aborta toda la transaccion: deliberadamente no permitimos PUTs
        // "parciales" en este endpoint para evitar inconsistencias.
        config.applyChatbotConfig(
                cmd.accessMode(),
                cmd.allowedRoles(),
                cmd.piiPolicy(),
                cmd.conversationBufferSize(),
                cmd.rateLimitAnonymousPerHour(),
                cmd.rateLimitAuthenticatedPerHour(),
                cmd.emergencyBannerText(),
                cmd.provideCitations(),
                cmd.showConfidenceIndicator());
        return configRepository.save(config);
    }

    /**
     * Limpia el cache de la URL de invocacion del agente para forzar el
     * siguiente descubrimiento via management API. Util cuando el operador
     * cambia el deployment en el dashboard de DigitalOcean.
     */
    public AiAgentConfiguration clearAgentInvocationUrlCache() {
        AiAgentConfiguration config = requireConfiguredAgent();
        config.clearAgentInvocationUrlCache();
        return configRepository.save(config);
    }

    @Override
    public AiAgentConfiguration revertToDraft() {
        AiAgentConfiguration config = requireConfiguredAgent();
        if (config.getLifecycle() != AiAgentLifecycle.PUBLISHED) {
            // Volver a DRAFT desde DRAFT/UNCONFIGURED no produce transicion real
            // y confunde al frontend: lo rechazamos con un codigo estable para
            // que pueda deshabilitar el boton "Revertir" segun el lifecycle.
            throw new InvalidBusinessRuleException(
                    "Solo se puede revertir a DRAFT una configuracion publicada.",
                    AiAgentErrorCodes.AI_AGENT_NOT_PUBLISHED);
        }
        config.markDraft();
        return configRepository.save(config);
    }

    @Override
    public AiAgentConfiguration publish(boolean override) {
        validateProviderAgentUuid();
        AiAgentConfiguration config = requireConfiguredAgent();
        AiGovernancePolicy policy = loadPolicyOrDefault();
        List<AgentPolicyRule> activeRules = policyRuleRepository.findAllActiveOrderByCreatedAtAsc();

        List<String> missing = computeMissingRequirements(config, policy, activeRules);
        boolean usedOverride = false;
        if (!missing.isEmpty()) {
            if (!override) {
                // La lista de requisitos faltantes viaja como details[] (codigos
                // estables) para que el frontend pueda pintarlos uno por uno
                // sin parsear el mensaje humano. El message sigue presente para
                // logs / soporte.
                throw new InvalidBusinessRuleException(
                        "No se puede publicar: faltan requisitos. Detalle en 'details'.",
                        AiAgentErrorCodes.AI_AGENT_CONFIG_INVALID,
                        missing);
            }
            if (!policy.isAllowOverride()) {
                throw new InvalidBusinessRuleException(
                        "Override solicitado pero la politica de gobernanza no lo permite. " +
                                "Habilite allowOverride=true en /governance antes de re-intentar.",
                        AiAgentErrorCodes.AI_AGENT_CONFIG_INVALID,
                        missing);
            }
            usedOverride = true;
        }

        String composedInstruction = config.composeInstruction(activeRules);

        AgentUpdateSpec spec = new AgentUpdateSpec(
                config.getDisplayName(),
                composedInstruction,
                config.getTemperature(),
                config.getTopP(),
                config.getMaxTokens(),
                config.getK(),
                config.getRetrievalMethod(),
                config.isProvideCitations()
        );

        Long actorId = resolveActorId();
        try {
            // El reconcile necesita conocer que guardrails estan REALMENTE
            // attached al agente en DO para calcular el diff con el intent
            // local. NO podemos usar el snapshot que devuelve updateAgent()
            // porque verificamos empiricamente que el response del PUT
            // /v2/gen-ai/agents/{uuid} NO incluye el campo "guardrails" (solo
            // viene en el GET). Sin este getAgent() previo, el detach se
            // quedaba con remoteAttachedUuids vacio y nunca se ejecutaba.
            AgentSnapshot preUpdateSnapshot = llmProvider.getAgent(providerAgentUuid);

            AgentSnapshot snapshot = llmProvider.updateAgent(providerAgentUuid, spec);
            // Calculamos el numero de version ANTES del reconcile para poder
            // referenciarlo en el evento de auditoria de fallo parcial si
            // alguna llamada attach/detach falla. El INSERT real de la version
            // ocurre mas abajo; este calculo solo predice el numero.
            int nextVersion = versionRepository.findMaxVersionNumber() + 1;

            // Reconciliar guardrails nativos del proveedor (RF31). Lo hacemos
            // DESPUES del updateAgent porque el spec no incluye guardrails:
            // se gestionan con endpoints attach/detach dedicados. Pasamos el
            // preUpdateSnapshot (que SI tiene el array guardrails poblado)
            // para calcular el diff correctamente. Si esto falla, lo logueamos
            // en audit (PROVIDER_GUARDRAIL_SYNC_PARTIAL_FAILURE) pero NO
            // marcamos publish failed: el agente ya tiene la instruction y
            // los parametros actualizados, lo critico esta hecho. El admin
            // puede reintentar la sincronizacion con otro publish.
            reconcileProviderGuardrails(preUpdateSnapshot, actorId, nextVersion);
            config.markPublished(snapshot.id() != null ? snapshot.id() : providerAgentUuid, Instant.now());
            AiAgentConfiguration saved = configRepository.save(config);

            String missingCsv = missing.isEmpty() ? null : String.join(",", missing);
            String guardrailsLabelsCsv = activeRules.stream()
                    .map(AgentPolicyRule::getLabel)
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
        AiAgentConfiguration config = requireConfiguredAgent();
        List<AgentPolicyRule> activeRules = policyRuleRepository.findAllActiveOrderByCreatedAtAsc();
        String composed = config.composeInstruction(activeRules);
        List<String> labels = activeRules.stream().map(AgentPolicyRule::getLabel).toList();
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
            List<AgentPolicyRule> activeRules = policyRuleRepository.findAllActiveOrderByCreatedAtAsc();
            missing = computeMissingRequirements(opt.get(), policy, activeRules);
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

        // Probe del endpoint del agente (chat completions). NO usa el circuit
        // breaker porque queremos el estado real, no el cached. Solo se prueba
        // si tenemos una URL conocida (ENV o cache); si no, dejamos los campos
        // null para que el FE muestre "no probado" en vez de "no alcanzable".
        Boolean agentReachable = null;
        String agentError = null;
        String invocationUrl = resolveAgentInvocationUrlForProbe(opt);
        if (invocationUrl != null) {
            site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.ProbeResult probe =
                    invokerPort.probe(invocationUrl);
            agentReachable = probe.reachable();
            agentError = probe.errorDetail();
        } else {
            agentError = "URL del agente desconocida (ENV vacia y cache de BD vacio).";
        }

        return new HealthResult(lifecycle, missing, reachable, providerError, agentReachable, agentError);
    }

    /**
     * Resuelve la URL del agente para el probe siguiendo la estrategia hibrida
     * (ENV gana, sino cache de BD). NO dispara descubrimiento via management API
     * para mantener {@code /health} barato: si la URL no esta resuelta el health
     * lo reporta y el admin la genera explicitamente con un PUT o limpiando el
     * cache.
     */
    private String resolveAgentInvocationUrlForProbe(Optional<AiAgentConfiguration> cfgOpt) {
        if (envAgentInvocationUrl != null && !envAgentInvocationUrl.isBlank()) {
            return envAgentInvocationUrl;
        }
        return cfgOpt.map(AiAgentConfiguration::getAgentInvocationUrl)
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);
    }

    /**
     * Calcula los requisitos faltantes para publicar segun la policy.
     * Devuelve lista vacia si todos los checks pasan.
     */
    private List<String> computeMissingRequirements(AiAgentConfiguration config,
                                                    AiGovernancePolicy policy,
                                                    List<AgentPolicyRule> activeRules) {
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
            int active = activeRules.size();
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
        // Chatbot en modo PRIVATE sin roles permitidos queda inaccesible para
        // todos: lo rechazamos en publish con un codigo estable para que el
        // FE pueda pintarlo como requisito explicito al lado del selector de
        // roles permitidos (RF29).
        if (config.getAccessMode() == site.utnpf.odontolink.domain.model.AiAgentAccessMode.PRIVATE
                && (config.getAllowedRoles() == null || config.getAllowedRoles().isEmpty())) {
            missing.add("REQUIRES_ALLOWED_ROLES_FOR_PRIVATE");
        }
        return missing;
    }

    /**
     * Resuelve la configuracion vigente o lanza 422 con {@code AI_AGENT_NOT_CONFIGURED}.
     * Se diferencia de un 404 generico porque el recurso si existe en el
     * contrato (el endpoint responde): lo que falta es la carga inicial por
     * parte del admin. El frontend ramifica para redirigir al wizard.
     */
    private AiAgentConfiguration requireConfiguredAgent() {
        return configRepository.findSingleton()
                .orElseThrow(() -> new InvalidBusinessRuleException(
                        "El agente IA aun no tiene configuracion. Cargue una via PUT /configuration antes de operar.",
                        AiAgentErrorCodes.AI_AGENT_NOT_CONFIGURED));
    }

    private AiGovernancePolicy loadPolicyOrDefault() {
        // El AiAgentSingletonBootstrapper siembra la fila al arranque, asi que
        // en operacion normal este metodo solo lee. Delegamos en el helper
        // para sobrevivir al caso degradado de borrado manual concurrente
        // (REQUIRES_NEW + catch duplicate-key + refetch).
        return singletonBootstrap.getOrCreate(
                policyRepository::findSingleton,
                AiGovernancePolicy::defaultStrict,
                policyRepository::save,
                "AiGovernancePolicy"
        );
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

    /**
     * Reconcilia el estado de los guardrails nativos del proveedor con la
     * intencion del admin (RF31).
     *
     * <p>El parametro {@code snapshot} debe ser el resultado de un
     * {@code getAgent()} (NO el de un {@code updateAgent()}): el PUT al
     * agente NO retorna el campo {@code guardrails} en su response — solo
     * el GET lo trae. Si pasamos el snapshot del PUT,
     * {@code remoteAttachedUuids} queda vacio y los detaches no se ejecutan.
     *
     * <p>Estrategia (alineada con la API real de DO Gradient, que es batch
     * para attach y singular para detach):
     * <ol>
     *   <li>Detach individual: para cada uuid attached en el proveedor que
     *       NO esta en el conjunto deseado del admin, hacer DELETE.</li>
     *   <li>Attach batch: si hay {@code >= 1} guardrail deseado, hacer UN
     *       POST con la lista completa. DO acepta re-attach idempotente, lo
     *       que aprovechamos tambien para actualizar priorities sin tener
     *       que detach+attach uno por uno.</li>
     *   <li>Re-leer el agente para confirmar el estado efectivo y persistir
     *       el espejo local con la realidad. Esto cierra el ciclo: si una
     *       operacion fallo silenciosamente, la BD refleja la verdad.</li>
     * </ol>
     *
     * <p>Fallos individuales (detach) o el batch (attach) se loguean a nivel
     * ERROR (no WARN) y se acumulan en una lista. Al final, si hubo fallos,
     * se registra un {@link AiAdminAuditEvent.Type#PROVIDER_GUARDRAIL_SYNC_PARTIAL_FAILURE}
     * con los UUIDs fallidos en {@code details}, para que el admin pueda
     * auditar via {@code GET /audit-events}. El publish principal NO se
     * aborta: instruction y parametros ya estan en DO; solo los guardrails
     * pueden quedar desincronizados (recuperable con otro publish o un
     * refresh).
     */
    private void reconcileProviderGuardrails(AgentSnapshot snapshot, Long actorUserId, int versionNumber) {
        List<site.utnpf.odontolink.domain.model.ProviderGuardrail> local =
                providerGuardrailRepository.findAllOrderByPriorityAsc();
        if (local.isEmpty()) {
            // Nada que reconciliar — el admin no toco el modulo. Conservamos
            // los attached que el proveedor ya tenia (no detach masivo).
            return;
        }

        // Construimos el conjunto deseado (intent local) y el remoto attached.
        List<ILlmAgentProviderPort.GuardrailAttachment> desired = new ArrayList<>();
        java.util.Set<String> desiredUuids = new java.util.HashSet<>();
        for (site.utnpf.odontolink.domain.model.ProviderGuardrail g : local) {
            if (g.isAttached()) {
                desired.add(new ILlmAgentProviderPort.GuardrailAttachment(
                        g.getProviderGuardrailUuid(), g.getPriority()));
                desiredUuids.add(g.getProviderGuardrailUuid());
            }
        }
        java.util.Set<String> remoteAttachedUuids = new java.util.HashSet<>();
        if (snapshot.guardrails() != null) {
            for (ILlmAgentProviderPort.ProviderGuardrailSnapshot r : snapshot.guardrails()) {
                if (r.isAttached() && r.guardrailUuid() != null) {
                    remoteAttachedUuids.add(r.guardrailUuid());
                }
            }
        }

        List<String> failures = new ArrayList<>();

        // 1) Detach individual de los que estan attached en DO pero no en el deseado.
        for (String remoteUuid : remoteAttachedUuids) {
            if (!desiredUuids.contains(remoteUuid)) {
                try {
                    llmProvider.detachGuardrail(providerAgentUuid, remoteUuid);
                    log.info("Detach guardrail {} (intent local: no attached).", remoteUuid);
                } catch (LlmProviderException ex) {
                    failures.add(remoteUuid + " (detach)");
                    log.error("Falla al detach guardrail {} en reconcile: code={}, message={}",
                            remoteUuid, ex.getErrorCode(), ex.getMessage());
                }
            }
        }

        // 2) Attach batch con TODO el conjunto deseado. La operacion es
        // idempotente del lado del proveedor para los que ya estaban
        // attached, asi nos ahorramos comparar priorities uno por uno.
        if (!desired.isEmpty()) {
            try {
                llmProvider.attachGuardrails(providerAgentUuid, desired);
                log.info("Attach batch enviado al proveedor: {} guardrails.", desired.size());
            } catch (LlmProviderException ex) {
                // No sabemos cual del batch fallo: marcamos todos como sospechosos.
                for (ILlmAgentProviderPort.GuardrailAttachment a : desired) {
                    failures.add(a.providerGuardrailUuid() + " (attach)");
                }
                log.error("Falla al attach batch ({} items): code={}, message={}",
                        desired.size(), ex.getErrorCode(), ex.getMessage());
            }
        }

        // 3) Re-leer la realidad de DO y persistir en el espejo local.
        // Esto cierra cualquier drift (incluso si las llamadas anteriores
        // fallaron silenciosamente o el proveedor mutó por su cuenta).
        try {
            AgentSnapshot post = llmProvider.getAgent(providerAgentUuid);
            java.util.Map<String, ILlmAgentProviderPort.ProviderGuardrailSnapshot> postByUuid =
                    new java.util.HashMap<>();
            if (post.guardrails() != null) {
                for (ILlmAgentProviderPort.ProviderGuardrailSnapshot s : post.guardrails()) {
                    if (s.guardrailUuid() != null) {
                        postByUuid.put(s.guardrailUuid(), s);
                    }
                }
            }
            for (site.utnpf.odontolink.domain.model.ProviderGuardrail g : local) {
                ILlmAgentProviderPort.ProviderGuardrailSnapshot s =
                        postByUuid.get(g.getProviderGuardrailUuid());
                if (s != null) {
                    g.setAttachmentIntent(s.isAttached(), s.priority());
                } else {
                    // DO ya no reporta el guardrail para este agente: lo
                    // tratamos como no attached (preservando la priority
                    // local por si vuelve a aparecer en un refresh futuro).
                    g.setAttachmentIntent(false, g.getPriority());
                }
                providerGuardrailRepository.save(g);
            }
        } catch (LlmProviderException ex) {
            log.error("Falla al re-leer el agente para confirmar reconcile: code={}, message={}. "
                            + "El espejo local puede quedar desactualizado hasta el proximo refresh.",
                    ex.getErrorCode(), ex.getMessage());
            failures.add("(post-fetch failed: " + ex.getErrorCode() + ")");
        }

        // 4) Si hubo fallos, registrar el evento de auditoria para que el admin
        // tenga rastro accionable. El publish sigue contado como exitoso.
        if (!failures.isEmpty()) {
            String detailsText = "Publish v" + versionNumber
                    + " — guardrails con falla de sync: " + String.join(", ", failures);
            auditRepository.save(AiAdminAuditEvent.of(
                    AiAdminAuditEvent.Type.PROVIDER_GUARDRAIL_SYNC_PARTIAL_FAILURE,
                    actorUserId,
                    versionNumber,
                    false,
                    detailsText));
        }
    }
}
