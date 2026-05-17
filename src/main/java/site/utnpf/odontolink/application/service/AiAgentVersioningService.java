package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAiAgentVersioningUseCase;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.AgentUpdateSpec;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;

/**
 * Servicio de aplicacion para versionado y rollback del agente IA (RF31).
 *
 * <p>El rollback NO modifica versiones antiguas: re-aplica el contenido
 * de una version anterior a la configuracion vigente, sincroniza con el
 * proveedor y genera una nueva version con numero correlativo. Asi el
 * historial queda lineal y entendible.
 */
@Transactional
public class AiAgentVersioningService implements IAiAgentVersioningUseCase {

    private final AiAgentConfigurationVersionRepository versionRepository;
    private final AiAgentConfigurationRepository configRepository;
    private final AiAdminAuditEventRepository auditRepository;
    private final ILlmAgentProviderPort llmProvider;
    private final AuthenticationFacade authFacade;
    private final String providerAgentUuid;

    public AiAgentVersioningService(AiAgentConfigurationVersionRepository versionRepository,
                                    AiAgentConfigurationRepository configRepository,
                                    AiAdminAuditEventRepository auditRepository,
                                    ILlmAgentProviderPort llmProvider,
                                    AuthenticationFacade authFacade,
                                    String providerAgentUuid) {
        this.versionRepository = versionRepository;
        this.configRepository = configRepository;
        this.auditRepository = auditRepository;
        this.llmProvider = llmProvider;
        this.authFacade = authFacade;
        this.providerAgentUuid = providerAgentUuid;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentConfigurationVersion> listVersions() {
        return versionRepository.findAllOrderByVersionNumberDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AiAgentConfigurationVersion> listVersionsPaged(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return versionRepository.findPaged(safePage, safeSize);
    }

    @Override
    public AiAgentConfigurationVersion rollbackToVersion(int versionNumber) {
        if (providerAgentUuid == null || providerAgentUuid.isBlank()) {
            throw new InvalidBusinessRuleException(
                    "El modulo de IA no esta configurado: falta DIGITALOCEAN_AGENT_UUID.");
        }

        AiAgentConfigurationVersion previous = versionRepository.findByVersionNumber(versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AiAgentConfigurationVersion", "versionNumber", String.valueOf(versionNumber)));

        AiAgentConfiguration config = configRepository.findSingleton()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AiAgentConfiguration", "singleton", "1"));

        // Re-aplicamos los valores de la version previa al agregado actual.
        // El metodo apply() los persiste y marca como DRAFT.
        config.apply(
                previous.getDisplayName(),
                previous.getSystemPromptCore(),
                previous.getWelcomeMessage(),
                previous.getTemperature(),
                previous.getTopP(),
                previous.getMaxTokens(),
                previous.getK(),
                previous.getRetrievalMethod()
        );

        // Sincronizamos al proveedor con la instruccion COMPOSED de la
        // version previa: incluye los textos exactos de guardrails que
        // existian al momento del publish original, aunque hoy ya no
        // esten en la tabla.
        AgentUpdateSpec spec = new AgentUpdateSpec(
                previous.getDisplayName(),
                previous.getComposedInstruction(),
                previous.getTemperature(),
                previous.getTopP(),
                previous.getMaxTokens(),
                previous.getK(),
                previous.getRetrievalMethod()
        );

        Long actorId = resolveActorId();
        try {
            llmProvider.updateAgent(providerAgentUuid, spec);
            config.markPublished(providerAgentUuid, Instant.now());
            configRepository.save(config);
        } catch (LlmProviderException ex) {
            config.markPublishFailed(ex.getMessage());
            configRepository.save(config);
            auditRepository.save(AiAdminAuditEvent.of(
                    AiAdminAuditEvent.Type.AGENT_PUBLISH_FAILED,
                    actorId,
                    versionNumber,
                    false,
                    "Rollback a v" + versionNumber + " fallo en proveedor: " + ex.getMessage()));
            throw ex;
        }

        int nextVersion = versionRepository.findMaxVersionNumber() + 1;
        AiAgentConfigurationVersion newVersion = new AiAgentConfigurationVersion(
                null,
                nextVersion,
                previous.getDisplayName(),
                previous.getSystemPromptCore(),
                previous.getWelcomeMessage(),
                previous.getTemperature(),
                previous.getTopP(),
                previous.getMaxTokens(),
                previous.getK(),
                previous.getRetrievalMethod(),
                previous.getComposedInstruction(),
                previous.getGuardrailsLabelsSnapshot(),
                actorId,
                false,
                null,
                Instant.now()
        );
        AiAgentConfigurationVersion saved = versionRepository.save(newVersion);

        auditRepository.save(AiAdminAuditEvent.of(
                AiAdminAuditEvent.Type.AGENT_ROLLBACK,
                actorId,
                nextVersion,
                false,
                "Rollback a v" + versionNumber + " creo v" + nextVersion));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAdminAuditEvent> listAuditEvents(int limit) {
        return auditRepository.findAllOrderByOccurredAtDesc(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AiAdminAuditEvent> listAuditEventsPaged(AiAdminAuditEvent.Type type,
                                                              Instant from,
                                                              Instant to,
                                                              int page,
                                                              int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        // type / from / to permanecen tal cual; null en cualquiera de los tres = sin filtro.
        return auditRepository.findPaged(type, from, to, safePage, safeSize);
    }

    private Long resolveActorId() {
        try {
            return authFacade.getAuthenticatedUser().getId();
        } catch (Exception ex) {
            return null;
        }
    }
}
