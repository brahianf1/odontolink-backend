package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.application.port.in.dto.ChatSessionView;
import site.utnpf.odontolink.application.port.in.dto.PagedMessages;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.ChatMessageRepository;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.service.ChatPolicyService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio de aplicación que orquesta los casos de uso del chat interno.
 *
 * Cubre RF26, RF27, RF28 y CU012 delegando todas las reglas de negocio al
 * {@link ChatPolicyService} (dominio) y los detalles de persistencia a los repositorios.
 *
 * Toda la clase es @Transactional: las operaciones que solo leen se marcan con readOnly
 * para que Hibernate pueda optimizar (flush mode = NEVER).
 *
 * @author OdontoLink Team
 */
@Transactional
public class ChatService implements IChatUseCase {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final ChatPolicyService chatPolicyService;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            PatientRepository patientRepository,
            PractitionerRepository practitionerRepository,
            ChatPolicyService chatPolicyService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.chatPolicyService = chatPolicyService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionView> getMyChatSessions(User user) {
        List<ChatSession> rawSessions = loadSessionsForUser(user);

        // Enriquecemos cada sesión con unreadCount y lastMessage. Esto cuesta O(N) queries por sesión,
        // pero en la práctica un usuario tiene un puñado de sesiones; preferimos claridad a un join
        // ad-hoc por ahora. Si el inbox crece se puede refactorizar a una projection JPQL única.
        List<ChatSessionView> enriched = new ArrayList<>(rawSessions.size());
        for (ChatSession session : rawSessions) {
            long unread = chatMessageRepository.countUnreadByChatSessionAndReceiver(session, user.getId());
            ChatMessage last = chatMessageRepository.findLastMessageInSession(session).orElse(null);
            enriched.add(new ChatSessionView(session, unread, last));
        }

        // Orden por actividad reciente: último mensaje > createdAt. Coloca arriba las conversaciones vivas.
        enriched.sort(Comparator.comparing(
                (ChatSessionView v) -> v.getLastMessage() != null
                        ? v.getLastMessage().getSentAt()
                        : v.getSession().getCreatedAt()
        ).reversed());

        return enriched;
    }

    @Override
    public ChatMessage sendMessage(Long chatSessionId, String content, User sender) {
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);

        // El policy service valida pertenencia + estado de bloqueo (RF28).
        chatPolicyService.validateMessageSend(chatSession, sender);

        ChatMessage newMessage = new ChatMessage(chatSession, sender, content);
        return chatMessageRepository.save(newMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long chatSessionId, User user, Instant sinceTimestamp) {
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateMessageAccess(chatSession, user);

        if (sinceTimestamp == null) {
            return chatMessageRepository.findByChatSessionOrderBySentAtAsc(chatSession);
        }
        return chatMessageRepository.findByChatSessionAndSentAtAfterOrderBySentAtAsc(chatSession, sinceTimestamp);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedMessages getMessagesPaged(Long chatSessionId, User user, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo.");
        }
        if (size <= 0 || size > 200) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 200.");
        }
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateMessageAccess(chatSession, user);

        List<ChatMessage> pageContent = chatMessageRepository.findByChatSessionPagedDesc(chatSession, page, size);
        long total = chatMessageRepository.countByChatSession(chatSession);
        return new PagedMessages(pageContent, page, size, total);
    }

    @Override
    public ChatSession blockChatSession(Long chatSessionId, User actor, String reason) {
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateBlockOperation(chatSession, actor);

        // Lógica de negocio en el modelo (Rich Domain): se asegura idempotencia, audit trail y consistencia.
        chatSession.block(actor, Role.ROLE_PRACTITIONER, normalizeReason(reason));
        return chatSessionRepository.save(chatSession);
    }

    @Override
    public ChatSession unblockChatSession(Long chatSessionId, User actor) {
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateUnblockOperation(chatSession, actor);

        chatSession.unblock();
        return chatSessionRepository.save(chatSession);
    }

    @Override
    public int markMessagesAsRead(Long chatSessionId, User receiver) {
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateMarkAsRead(chatSession, receiver);

        // Bulk UPDATE en una sola sentencia SQL → evita N+1 al abrir conversaciones largas.
        return chatMessageRepository.markAllAsReadInSession(chatSession, receiver.getId(), Instant.now());
    }

    // Helpers privados

    private ChatSession loadSessionOrThrow(Long chatSessionId) {
        return chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", chatSessionId.toString()));
    }

    private List<ChatSession> loadSessionsForUser(User user) {
        if (user.getRole() == Role.ROLE_PATIENT) {
            Patient patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", user.getId().toString()));
            return chatSessionRepository.findByPatient(patient);
        }
        if (user.getRole() == Role.ROLE_PRACTITIONER) {
            Practitioner practitioner = practitionerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "userId", user.getId().toString()));
            return chatSessionRepository.findByPractitioner(practitioner);
        }
        throw new IllegalArgumentException("Solo los pacientes y practicantes pueden acceder al chat.");
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
