package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.utnpf.odontolink.application.port.in.dto.ChatbotMessageCommand;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.AgentInvocationResult;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.ChatMessage;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.service.security.EmergencyDetector;
import site.utnpf.odontolink.application.service.security.PiiSanitizer;
import site.utnpf.odontolink.domain.model.AiAgentAccessMode;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiPiiPolicy;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.ChatbotMessage;
import site.utnpf.odontolink.domain.model.ChatbotMessageRole;
import site.utnpf.odontolink.domain.model.ChatbotSession;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.ChatbotMessageRepository;
import site.utnpf.odontolink.domain.repository.ChatbotSessionRepository;
import site.utnpf.odontolink.domain.repository.EmergencyKeywordRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del orquestador del chatbot (RF29/RF31/RF32/RF34).
 *
 * <p>Foco principal: <strong>regresion del wire format contra DigitalOcean
 * Gradient con {@code ?agent=true}</strong>. DO rechaza con 400 cualquier
 * mensaje con {@code role=system} o {@code role=developer} cuando se invoca
 * el endpoint del agente. Las instrucciones del agente viven server-side y
 * se sincronizan al hacer {@code POST /publish}. Estos tests garantizan que
 * el use case NUNCA inyecte un system inline al invocador.
 */
class ChatbotInteractionServiceTest {

    private AiAgentConfigurationRepository configRepo;
    private ChatbotSessionRepository sessionRepo;
    private ChatbotMessageRepository messageRepo;
    private EmergencyKeywordRepository emergencyKeywordRepo;
    private ILlmAgentInvokerPort invokerPort;
    private ILlmAgentProviderPort providerPort;
    private ChatbotInteractionService service;

    @BeforeEach
    void setUp() {
        configRepo = mock(AiAgentConfigurationRepository.class);
        sessionRepo = mock(ChatbotSessionRepository.class);
        messageRepo = mock(ChatbotMessageRepository.class);
        emergencyKeywordRepo = mock(EmergencyKeywordRepository.class);
        invokerPort = mock(ILlmAgentInvokerPort.class);
        providerPort = mock(ILlmAgentProviderPort.class);

        // Calculator real (no mockeado): es un servicio puro y barato; nos
        // interesa que los tests del servicio reflejen la composicion real
        // con su configuracion default razonable.
        site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig confidenceConfig =
                new site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig(
                        10.0, 0.7, 0.3, 0.4, 0.5, 0.4, 3,
                        java.util.List.of(0.5, 0.3, 0.2),
                        0.5, 0.7, 30,
                        java.util.List.of("no puedo procesar esta solicitud"),
                        new site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig.CategoryMessages(
                                "Información oficial", "off body",
                                "Información parcial", "par body",
                                "Respuesta general",   "gen body",
                                "Fuera de alcance",    "out body"));
        site.utnpf.odontolink.domain.service.RefusalDetector refusal =
                new site.utnpf.odontolink.domain.service.RefusalDetector(confidenceConfig);
        site.utnpf.odontolink.domain.service.ConfidenceCalculator calculator =
                new site.utnpf.odontolink.domain.service.ConfidenceCalculator(confidenceConfig, refusal);

        service = new ChatbotInteractionService(
                configRepo,
                sessionRepo,
                messageRepo,
                emergencyKeywordRepo,
                new PiiSanitizer(),
                new EmergencyDetector(),
                invokerPort,
                providerPort,
                calculator,
                "https://test.agents.do-ai.run",
                "agent-uuid"
        );
    }

    @Test
    void wireNoIncluyeRoleSystem() {
        // Setup: config publicada, sesion anonima nueva.
        AiAgentConfiguration cfg = publishedConfig();
        when(configRepo.findSingleton()).thenReturn(Optional.of(cfg));
        when(emergencyKeywordRepo.findAllActive()).thenReturn(List.of());

        ChatbotSession session = ChatbotSession.forAnonymous();
        when(sessionRepo.save(any())).thenReturn(session);
        when(messageRepo.findLastNBySessionId(any(), anyInt()))
                .thenReturn(List.of(ChatbotMessage.createNew(session.getId(),
                        ChatbotMessageRole.USER, "hola")));

        when(invokerPort.invoke(anyString(), any()))
                .thenReturn(new AgentInvocationResult("hola, ¿en qué te ayudo?", List.of(), 5));

        // Act
        service.sendMessage(new ChatbotMessageCommand(
                "hola",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "1.2.3.4"
        ));

        // Assert: capturar lo que enviamos al invocador.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor =
                ArgumentCaptor.forClass((Class<List<ChatMessage>>) (Class<?>) List.class);
        verify(invokerPort).invoke(eq("https://test.agents.do-ai.run"), captor.capture());
        List<ChatMessage> wire = captor.getValue();

        assertNotNull(wire);
        assertFalse(wire.isEmpty(), "El wire debe contener al menos el mensaje del usuario.");
        for (ChatMessage m : wire) {
            assertFalse("system".equalsIgnoreCase(m.role()),
                    "El wire NO debe contener role=system (DO rechaza con 400 bajo ?agent=true).");
            assertFalse("developer".equalsIgnoreCase(m.role()),
                    "El wire NO debe contener role=developer (DO rechaza con 400 bajo ?agent=true).");
        }
    }

    @Test
    void wirePreservaOrdenCronologicoUserAssistantTurns() {
        AiAgentConfiguration cfg = publishedConfig();
        when(configRepo.findSingleton()).thenReturn(Optional.of(cfg));
        when(emergencyKeywordRepo.findAllActive()).thenReturn(List.of());

        UUID sid = UUID.randomUUID();
        ChatbotSession session = new ChatbotSession(sid, null, UUID.randomUUID(),
                Instant.now().minusSeconds(60), Instant.now().minusSeconds(10), 2);
        when(sessionRepo.findById(sid)).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenReturn(session);

        // Buffer rolling: alternancia user/assistant historica.
        Instant t0 = Instant.now().minusSeconds(30);
        when(messageRepo.findLastNBySessionId(eq(sid), anyInt())).thenReturn(List.of(
                new ChatbotMessage(1L, sid, ChatbotMessageRole.USER, "anterior 1", t0),
                new ChatbotMessage(2L, sid, ChatbotMessageRole.ASSISTANT, "respuesta 1", t0.plusSeconds(1)),
                new ChatbotMessage(3L, sid, ChatbotMessageRole.USER, "hola de nuevo", t0.plusSeconds(2))
        ));

        when(invokerPort.invoke(anyString(), any()))
                .thenReturn(new AgentInvocationResult("ok", List.of(), 5));

        service.sendMessage(new ChatbotMessageCommand(
                "hola de nuevo",
                Optional.of(sid),
                Optional.of(session.getAnonymousToken()),
                Optional.empty(),
                "1.2.3.4"
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor =
                ArgumentCaptor.forClass((Class<List<ChatMessage>>) (Class<?>) List.class);
        verify(invokerPort).invoke(anyString(), captor.capture());
        List<ChatMessage> wire = captor.getValue();

        assertEquals(3, wire.size(),
                "El wire debe contener exactamente los mensajes del rolling buffer, sin extras.");
        assertEquals("user", wire.get(0).role());
        assertEquals("assistant", wire.get(1).role());
        assertEquals("user", wire.get(2).role());
    }

    /** Helper: configuracion publicada con accessMode PUBLIC para que el flujo no rechace. */
    private AiAgentConfiguration publishedConfig() {
        AiAgentConfiguration cfg = new AiAgentConfiguration(
                AiAgentConfiguration.SINGLETON_ID,
                "Asistente Test",
                "Eres el asistente de prueba.",
                "Hola!",
                new BigDecimal("0.7"),
                new BigDecimal("0.9"),
                256,
                10,
                AiRetrievalMethod.REWRITE,
                AiAgentLifecycle.PUBLISHED,
                "agent-uuid",
                Instant.now().minusSeconds(60),
                null,
                Instant.now().minusSeconds(60),
                AiAgentAccessMode.PUBLIC,
                Collections.emptySet(),
                AiPiiPolicy.BLOCK,
                20,
                20,
                60,
                "https://test.agents.do-ai.run",
                "*** Emergencia ***",
                false,
                true
        );
        cfg.setId(AiAgentConfiguration.SINGLETON_ID);
        // Forzamos EnumSet vacio para evitar Collections.emptySet() en el constructor copy.
        return new AiAgentConfiguration(
                cfg.getId(),
                cfg.getDisplayName(),
                cfg.getSystemPromptCore(),
                cfg.getWelcomeMessage(),
                cfg.getTemperature(),
                cfg.getTopP(),
                cfg.getMaxTokens(),
                cfg.getK(),
                cfg.getRetrievalMethod(),
                cfg.getLifecycle(),
                cfg.getProviderAgentId(),
                cfg.getProviderSyncedAt(),
                cfg.getLastSyncError(),
                cfg.getUpdatedAt(),
                cfg.getAccessMode(),
                EnumSet.noneOf(site.utnpf.odontolink.domain.model.Role.class),
                cfg.getPiiPolicy(),
                cfg.getConversationBufferSize(),
                cfg.getRateLimitAnonymousPerHour(),
                cfg.getRateLimitAuthenticatedPerHour(),
                cfg.getAgentInvocationUrl(),
                cfg.getEmergencyBannerText(),
                cfg.isProvideCitations(),
                cfg.isShowConfidenceIndicator()
        );
    }
}
