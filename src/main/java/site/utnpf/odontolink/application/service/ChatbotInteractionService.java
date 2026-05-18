package site.utnpf.odontolink.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IChatbotInteractionUseCase;
import site.utnpf.odontolink.application.port.in.dto.ChatbotMessageCommand;
import site.utnpf.odontolink.application.port.in.dto.ChatbotPublicInfo;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.AgentInvocationResult;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.ChatMessage;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.RetrievalDocument;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.service.security.EmergencyDetector;
import site.utnpf.odontolink.application.service.security.PiiSanitizer;
import site.utnpf.odontolink.application.service.security.PiiSanitizer.PiiScanResult;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AiAgentAccessMode;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiPiiPolicy;
import site.utnpf.odontolink.domain.model.ChatbotInteractionResult;
import site.utnpf.odontolink.domain.model.ChatbotMessage;
import site.utnpf.odontolink.domain.model.ChatbotMessageRole;
import site.utnpf.odontolink.domain.model.ChatbotSession;
import site.utnpf.odontolink.domain.model.EmergencyKeyword;
import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.ChatbotMessageRepository;
import site.utnpf.odontolink.domain.repository.ChatbotSessionRepository;
import site.utnpf.odontolink.domain.repository.EmergencyKeywordRepository;
import site.utnpf.odontolink.domain.repository.GuardrailRepository;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de aplicacion del chatbot institucional (RF29/RF31/RF32/RF34).
 *
 * <p>Orquesta el flujo completo: validar acceso, resolver/crear sesion,
 * sanitizar PII, detectar emergencias, persistir el mensaje del usuario en
 * el rolling buffer, llamar al proveedor con resiliencia (el adapter aplica
 * @CircuitBreaker), procesar la respuesta, mapear confidence y persistir la
 * respuesta del bot.
 *
 * <p>Decisiones clave:
 * <ul>
 *   <li>Si el proveedor falla y se dispara fallback ({@link LlmProviderException}):
 *       devolvemos HTTP 200 con {@code fallbackTriggered=true} para que la UX
 *       no se rompa. No persistimos la respuesta fallback en el buffer.</li>
 *   <li>Si la politica PII es BLOCK y se detecto PII: respondemos un mensaje
 *       educativo y NO llamamos al proveedor. No persistimos el mensaje del
 *       usuario (evita guardar PII en BD).</li>
 *   <li>El rolling buffer se purga FIFO despues de cada turno exitoso.</li>
 * </ul>
 */
@Transactional
public class ChatbotInteractionService implements IChatbotInteractionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatbotInteractionService.class);

    private static final String PII_EDUCATIONAL_REPLY =
            "Por tu seguridad, te pedimos que no compartas datos personales sensibles " +
                    "(DNI, CUIT, CBU, numeros de tarjeta, etc.) en este canal. " +
                    "Reformula tu consulta sin esa informacion y con gusto te ayudo.";

    private static final String FALLBACK_REPLY =
            "El asistente no esta disponible en este momento. " +
                    "Si tu consulta es urgente, por favor contactate con la clinica.";

    private final AiAgentConfigurationRepository configRepository;
    private final GuardrailRepository guardrailRepository;
    private final ChatbotSessionRepository sessionRepository;
    private final ChatbotMessageRepository messageRepository;
    private final EmergencyKeywordRepository emergencyKeywordRepository;
    private final PiiSanitizer piiSanitizer;
    private final EmergencyDetector emergencyDetector;
    private final ILlmAgentInvokerPort invokerPort;
    private final ILlmAgentProviderPort providerPort;
    private final String envAgentInvocationUrl;
    private final String providerAgentUuid;

    public ChatbotInteractionService(AiAgentConfigurationRepository configRepository,
                                     GuardrailRepository guardrailRepository,
                                     ChatbotSessionRepository sessionRepository,
                                     ChatbotMessageRepository messageRepository,
                                     EmergencyKeywordRepository emergencyKeywordRepository,
                                     PiiSanitizer piiSanitizer,
                                     EmergencyDetector emergencyDetector,
                                     ILlmAgentInvokerPort invokerPort,
                                     ILlmAgentProviderPort providerPort,
                                     String envAgentInvocationUrl,
                                     String providerAgentUuid) {
        this.configRepository = configRepository;
        this.guardrailRepository = guardrailRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.emergencyKeywordRepository = emergencyKeywordRepository;
        this.piiSanitizer = piiSanitizer;
        this.emergencyDetector = emergencyDetector;
        this.invokerPort = invokerPort;
        this.providerPort = providerPort;
        this.envAgentInvocationUrl = envAgentInvocationUrl;
        this.providerAgentUuid = providerAgentUuid;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatbotPublicInfo getPublicInfo(Role callerRole) {
        Optional<AiAgentConfiguration> opt = configRepository.findSingleton();
        if (opt.isEmpty()) {
            return ChatbotPublicInfo.denied(AiAgentAccessMode.DISABLED, "AGENT_NOT_PUBLISHED");
        }
        AiAgentConfiguration config = opt.get();
        if (config.getLifecycle() != AiAgentLifecycle.PUBLISHED) {
            return ChatbotPublicInfo.denied(config.getAccessMode(), "AGENT_NOT_PUBLISHED");
        }
        if (config.getAccessMode() == AiAgentAccessMode.DISABLED) {
            return ChatbotPublicInfo.denied(AiAgentAccessMode.DISABLED, "AGENT_DISABLED");
        }
        if (config.getAccessMode() == AiAgentAccessMode.PRIVATE) {
            if (callerRole == null) {
                return ChatbotPublicInfo.denied(AiAgentAccessMode.PRIVATE, "AUTHENTICATION_REQUIRED");
            }
            if (!config.getAllowedRoles().contains(callerRole)) {
                return ChatbotPublicInfo.denied(AiAgentAccessMode.PRIVATE, "ROLE_NOT_ALLOWED");
            }
        }
        return ChatbotPublicInfo.granted(
                config.getAccessMode(),
                config.getDisplayName(),
                config.getWelcomeMessage());
    }

    @Override
    public ChatbotInteractionResult sendMessage(ChatbotMessageCommand cmd) {
        long startedAt = System.currentTimeMillis();
        AiAgentConfiguration config = requirePublishedConfig();
        validateAccess(config, cmd);

        // Sesion: cargar y validar ownership, o crear nueva.
        ChatbotSession session = resolveSession(config, cmd);

        // Sanitizacion PII pre-envio.
        PiiScanResult pii = piiSanitizer.scan(cmd.message());
        if (pii.hasPii() && config.getPiiPolicy() == AiPiiPolicy.BLOCK) {
            // No persistimos el mensaje original ni llamamos al proveedor.
            long latency = System.currentTimeMillis() - startedAt;
            return ChatbotInteractionResult.piiBlocked(
                    session.getId(), session.getAnonymousToken(),
                    PII_EDUCATIONAL_REPLY, pii.detected(), latency);
        }
        // Politica ANONYMIZE (o sin PII): usamos el texto sanitizado para todo
        // el flujo posterior (BD y proveedor). El original solo queda en el
        // log de la request (no persistido).
        String userVisibleMessage = pii.hasPii() ? pii.sanitized() : cmd.message();

        // Deteccion local de emergencias (independiente de los guardrails del proveedor).
        List<EmergencyKeyword> activeKeywords = emergencyKeywordRepository.findAllActive();
        boolean emergency = emergencyDetector.containsEmergencyTerm(userVisibleMessage, activeKeywords);

        // Persistimos el turno del usuario en el buffer rolling.
        ChatbotMessage userMessage = ChatbotMessage.createNew(
                session.getId(), ChatbotMessageRole.USER, userVisibleMessage);
        messageRepository.save(userMessage);

        // Construimos el historial para el proveedor: system + ultimos N
        // mensajes (que ya incluyen el del usuario recien persistido).
        List<ChatMessage> wireMessages = buildWireMessages(config, session.getId());

        // Llamada al proveedor con resiliencia (el adapter aplica el circuit
        // breaker). Cualquier excepcion irrecuperable cae al fallback.
        ChatbotInteractionResult result;
        try {
            String url = resolveAgentInvocationUrl(config);
            AgentInvocationResult invocation = invokerPort.invoke(url, wireMessages);
            result = buildResult(session, invocation, emergency, config, pii, startedAt);

            // Persistimos la respuesta del bot solo si NO es fallback (en
            // fallback ya devolvimos antes; aqui no aplica). Aplicamos banner
            // antes de persistir para que el rolling buffer conserve lo que
            // realmente vio el usuario.
            ChatbotMessage botMessage = ChatbotMessage.createNew(
                    session.getId(), ChatbotMessageRole.ASSISTANT, result.reply());
            messageRepository.save(botMessage);
        } catch (LlmProviderException ex) {
            log.warn("Fallback del chatbot por falla del proveedor: code={} message={}",
                    ex.getErrorCode(), ex.getMessage());
            long latency = System.currentTimeMillis() - startedAt;
            return ChatbotInteractionResult.fallback(
                    session.getId(), session.getAnonymousToken(), FALLBACK_REPLY, latency);
        }

        // Sesion + cap FIFO. recordInteraction++ y refresh timestamp.
        session.recordInteraction();
        sessionRepository.save(session);
        messageRepository.deleteOldestKeepingLast(session.getId(), config.getConversationBufferSize());

        return result;
    }

    @Override
    public void closeSession(UUID sessionId, Optional<Long> authenticatedUserId, Optional<UUID> anonymousToken) {
        Optional<ChatbotSession> opt = sessionRepository.findById(sessionId);
        if (opt.isEmpty()) {
            // Idempotente.
            return;
        }
        ChatbotSession session = opt.get();
        if (!session.isAccessibleBy(authenticatedUserId.orElse(null), anonymousToken.orElse(null))) {
            // 404 en lugar de 403 para no confirmar existencia.
            throw new ResourceNotFoundException("ChatbotSession", "id", sessionId.toString());
        }
        messageRepository.deleteAllBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    // --- Helpers privados ---------------------------------------------------

    private AiAgentConfiguration requirePublishedConfig() {
        AiAgentConfiguration config = configRepository.findSingleton()
                .orElseThrow(() -> new LlmProviderException(
                        "El asistente todavia no esta configurado.", null,
                        AiAgentErrorCodes.AI_AGENT_NOT_PUBLISHED));
        if (config.getLifecycle() != AiAgentLifecycle.PUBLISHED) {
            throw new LlmProviderException(
                    "El asistente esta en preparacion. Volve mas tarde.", null,
                    AiAgentErrorCodes.AI_AGENT_NOT_PUBLISHED);
        }
        return config;
    }

    private void validateAccess(AiAgentConfiguration config, ChatbotMessageCommand cmd) {
        if (config.getAccessMode() == AiAgentAccessMode.DISABLED) {
            throw new LlmProviderException(
                    "El chatbot esta deshabilitado en este momento.", null,
                    AiAgentErrorCodes.AI_AGENT_DISABLED);
        }
        if (config.getAccessMode() == AiAgentAccessMode.PRIVATE) {
            if (cmd.authenticatedUserId().isEmpty()) {
                throw new LlmProviderException(
                        "El chatbot requiere autenticacion en este momento.", null,
                        AiAgentErrorCodes.AI_AGENT_ANONYMOUS_FORBIDDEN);
            }
            // Validacion fina del rol queda fuera del use case: el controller
            // resolvio el role y lo pasa via getPublicInfo() para que el FE
            // sepa antes de invocar. Si llega autenticado pero sin rol,
            // el filter o el controller deberian haber rechazado antes; aqui
            // no validamos rol porque el use case no recibe el Role directo
            // (lo hace el path GET /info, mantenemos contrato consistente).
        }
    }

    private ChatbotSession resolveSession(AiAgentConfiguration config, ChatbotMessageCommand cmd) {
        if (cmd.sessionId().isPresent()) {
            UUID sid = cmd.sessionId().get();
            ChatbotSession existing = sessionRepository.findById(sid)
                    .orElseThrow(() -> new ResourceNotFoundException("ChatbotSession", "id", sid.toString()));
            if (!existing.isAccessibleBy(cmd.authenticatedUserId().orElse(null),
                    cmd.anonymousToken().orElse(null))) {
                throw new ResourceNotFoundException("ChatbotSession", "id", sid.toString());
            }
            return existing;
        }
        // Nueva sesion: distinguimos autenticado vs anonimo. Si modo es PRIVATE
        // el authenticatedUserId ya esta presente (validamos arriba); si es
        // PUBLIC y el caller esta autenticado, preferimos la sesion de usuario
        // (preserva memoria conversacional entre sesiones de browser).
        if (cmd.authenticatedUserId().isPresent()) {
            return sessionRepository.save(ChatbotSession.forUser(cmd.authenticatedUserId().get()));
        }
        // Anonimo: rechazamos si el modo es PRIVATE (defensa adicional al
        // validateAccess que ya corrio).
        if (config.getAccessMode() == AiAgentAccessMode.PRIVATE) {
            throw new LlmProviderException(
                    "El chatbot requiere autenticacion.", null,
                    AiAgentErrorCodes.AI_AGENT_ANONYMOUS_FORBIDDEN);
        }
        return sessionRepository.save(ChatbotSession.forAnonymous());
    }

    /**
     * Compone el wire-format que viaja al proveedor: instruccion (system) +
     * ultimos N mensajes en orden cronologico ASC. El system se compone con
     * los guardrails activos para garantizar las protecciones clinicas en
     * cada turno (no son sticky desde el punto de vista del LLM).
     */
    private List<ChatMessage> buildWireMessages(AiAgentConfiguration config, UUID sessionId) {
        List<Guardrail> activeGuardrails = guardrailRepository.findAllActiveOrderByCreatedAtAsc();
        String composedInstruction = config.composeInstruction(activeGuardrails);

        List<ChatbotMessage> recent = messageRepository.findLastNBySessionId(
                sessionId, config.getConversationBufferSize());
        // Garantizamos orden cronologico ASC defensivamente.
        recent.sort(Comparator.comparing(ChatbotMessage::getCreatedAt));

        List<ChatMessage> wire = new ArrayList<>(recent.size() + 1);
        wire.add(new ChatMessage("system", composedInstruction));
        for (ChatbotMessage m : recent) {
            String role = m.getRole() == ChatbotMessageRole.USER ? "user" : "assistant";
            wire.add(new ChatMessage(role, m.getContent()));
        }
        return wire;
    }

    /**
     * Resuelve la URL de invocacion del agente segun la estrategia hibrida:
     * <ol>
     *   <li>ENV {@code DIGITALOCEAN_AGENT_INVOCATION_URL} si esta seteada.</li>
     *   <li>Cache local en {@code config.agentInvocationUrl} si esta presente.</li>
     *   <li>Descubrimiento via management API ({@code getAgent}) + persistencia
     *       en config.</li>
     * </ol>
     */
    private String resolveAgentInvocationUrl(AiAgentConfiguration config) {
        if (envAgentInvocationUrl != null && !envAgentInvocationUrl.isBlank()) {
            // ENV gana. Mantenemos el cache de BD sincronizado solo si esta
            // vacio o desactualizado, para que el endpoint admin de "clear
            // cache" tenga visibilidad consistente.
            if (!envAgentInvocationUrl.equals(config.getAgentInvocationUrl())) {
                config.cacheAgentInvocationUrl(envAgentInvocationUrl);
                configRepository.save(config);
            }
            return envAgentInvocationUrl;
        }
        if (config.getAgentInvocationUrl() != null && !config.getAgentInvocationUrl().isBlank()) {
            return config.getAgentInvocationUrl();
        }
        if (providerAgentUuid == null || providerAgentUuid.isBlank()) {
            throw new LlmProviderException(
                    "No hay URL de invocacion configurada y no se puede descubrir (DIGITALOCEAN_AGENT_UUID vacio).",
                    null,
                    AiAgentErrorCodes.AI_AGENT_INVOCATION_URL_UNAVAILABLE);
        }
        try {
            ILlmAgentProviderPort.AgentSnapshot snap = providerPort.getAgent(providerAgentUuid);
            String discovered = snap.deploymentEndpoint();
            if (discovered == null || discovered.isBlank()) {
                throw new LlmProviderException(
                        "El proveedor no reporto deployment.url para el agente.", null,
                        AiAgentErrorCodes.AI_AGENT_INVOCATION_URL_UNAVAILABLE);
            }
            config.cacheAgentInvocationUrl(discovered);
            configRepository.save(config);
            return discovered;
        } catch (LlmProviderException ex) {
            // Re-propagamos con el codigo correcto si vino otro generico.
            if (ex.getErrorCode() == null) {
                throw new LlmProviderException(
                        ex.getMessage(), ex.getStatusCode(),
                        AiAgentErrorCodes.AI_AGENT_INVOCATION_URL_UNAVAILABLE, ex);
            }
            throw ex;
        }
    }

    private ChatbotInteractionResult buildResult(ChatbotSession session,
                                                AgentInvocationResult invocation,
                                                boolean emergency,
                                                AiAgentConfiguration config,
                                                PiiScanResult pii,
                                                long startedAt) {
        String reply = invocation.reply();
        if (reply == null || reply.isBlank()) {
            reply = FALLBACK_REPLY;
        }
        Integer confidence;
        boolean basedOnKb;
        List<RetrievalDocument> docs = invocation.retrievedDocuments() == null
                ? List.of() : invocation.retrievedDocuments();
        if (docs.isEmpty()) {
            // Sin RAG: respuesta general. Confidence neutro para que el FE
            // muestre badge "respuesta general" si quiere.
            confidence = 50;
            basedOnKb = false;
        } else {
            double maxScore = docs.stream().mapToDouble(RetrievalDocument::score).max().orElse(0.0);
            confidence = (int) Math.round(Math.max(0.0, Math.min(1.0, maxScore)) * 100.0);
            basedOnKb = true;
        }
        if (emergency) {
            reply = config.getEmergencyBannerText() + reply;
            confidence = null; // En emergencias el foco es la derivacion, no la confianza.
        }
        long latency = System.currentTimeMillis() - startedAt;
        List<String> retrievedIds = docs.stream().map(RetrievalDocument::dataSourceId).toList();
        Set<site.utnpf.odontolink.domain.model.ChatbotPiiType> detected = pii == null
                ? Set.of() : pii.detected();
        return new ChatbotInteractionResult(
                session.getId(),
                session.getAnonymousToken(),
                reply,
                confidence,
                basedOnKb,
                emergency,
                false,
                detected,
                false,
                latency,
                retrievedIds
        );
    }
}
