package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAiGovernancePolicyUseCase;
import site.utnpf.odontolink.application.service.support.SingletonRowBootstrap;
import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

/**
 * Servicio de aplicacion para la {@link AiGovernancePolicy} (RF31).
 *
 * <p>Cada update se registra en el audit log con el delta del cambio. Esto
 * es importante porque modificar la policy (sobre todo prender
 * {@code allowOverride}) tiene consecuencias graves: queda visible quien y
 * cuando lo hizo.
 *
 * <p>El bootstrap eager ({@code AiAgentSingletonBootstrapper}) garantiza que
 * la fila singleton exista al arranque. El {@code getPolicy()} aqui delega
 * cualquier path "find or create" en {@link SingletonRowBootstrap}, que abre
 * transacciones nuevas (REQUIRES_NEW) para sobrevivir a races concurrentes
 * — defensa por si la fila se borra a mano post-deploy.
 */
@Transactional
public class AiGovernancePolicyService implements IAiGovernancePolicyUseCase {

    private final AiGovernancePolicyRepository repository;
    private final AiAdminAuditEventRepository auditRepository;
    private final AuthenticationFacade authFacade;
    private final SingletonRowBootstrap bootstrap;

    public AiGovernancePolicyService(AiGovernancePolicyRepository repository,
                                     AiAdminAuditEventRepository auditRepository,
                                     AuthenticationFacade authFacade,
                                     SingletonRowBootstrap bootstrap) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.authFacade = authFacade;
        this.bootstrap = bootstrap;
    }

    @Override
    @Transactional(readOnly = true)
    public AiGovernancePolicy getPolicy() {
        return bootstrap.getOrCreate(
                repository::findSingleton,
                AiGovernancePolicy::defaultStrict,
                repository::save,
                "AiGovernancePolicy"
        );
    }

    @Override
    public AiGovernancePolicy updatePolicy(boolean requireGuardrails,
                                           int minActiveGuardrails,
                                           boolean requireSystemPrompt,
                                           boolean requireWelcomeMessage,
                                           boolean requireIndexedDocuments,
                                           boolean allowOverride) {
        AiGovernancePolicy policy = getPolicy();
        boolean previousAllowOverride = policy.isAllowOverride();
        policy.apply(requireGuardrails, minActiveGuardrails, requireSystemPrompt,
                requireWelcomeMessage, requireIndexedDocuments, allowOverride);
        AiGovernancePolicy saved = repository.save(policy);

        Long actorId = resolveActorId();
        String details = "Policy actualizada. "
                + "requireGuardrails=" + requireGuardrails
                + ", minActiveGuardrails=" + minActiveGuardrails
                + ", requireSystemPrompt=" + requireSystemPrompt
                + ", requireWelcomeMessage=" + requireWelcomeMessage
                + ", requireIndexedDocuments=" + requireIndexedDocuments
                + ", allowOverride=" + allowOverride
                + (previousAllowOverride != allowOverride
                        ? " (allowOverride cambio de " + previousAllowOverride + " a " + allowOverride + ")"
                        : "");
        auditRepository.save(AiAdminAuditEvent.of(
                AiAdminAuditEvent.Type.GOVERNANCE_POLICY_UPDATED,
                actorId,
                null,
                false,
                details));
        return saved;
    }

    private Long resolveActorId() {
        try {
            return authFacade.getAuthenticatedUser().getId();
        } catch (Exception ex) {
            return null;
        }
    }
}
