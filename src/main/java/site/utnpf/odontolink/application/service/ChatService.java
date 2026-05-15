package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.application.port.in.dto.ChatPollResult;
import site.utnpf.odontolink.application.port.in.dto.ChatSessionView;
import site.utnpf.odontolink.application.port.in.dto.PagedMessages;
import site.utnpf.odontolink.application.port.in.dto.ReadReceipt;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.ChatMessageRepository;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.service.ChatPolicyService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final String CODE_NO_PRIOR_RELATIONSHIP = "CHAT_NO_PRIOR_RELATIONSHIP";
    private static final String CODE_PARTICIPANT_MISMATCH = "CHAT_PARTICIPANT_MISMATCH";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final AppointmentRepository appointmentRepository;
    private final ChatPolicyService chatPolicyService;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            PatientRepository patientRepository,
            PractitionerRepository practitionerRepository,
            AppointmentRepository appointmentRepository,
            ChatPolicyService chatPolicyService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.appointmentRepository = appointmentRepository;
        this.chatPolicyService = chatPolicyService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionView> getMyChatSessions(User user, Instant since) {
        List<ChatSession> rawSessions = loadSessionsForUser(user);

        // Enriquecemos cada sesión con unreadCount y lastMessage. Cuesta O(N) queries por sesión,
        // pero un usuario suele tener un puñado de conversaciones; si crece se puede colapsar a
        // una projection JPQL única.
        //
        // Filtro 'since' (P2): solo incluir si hubo actividad nueva relevante. Una sesión
        // "cambió" en el sentido inbox cuando llegó un mensaje desde el cursor. Marcar mensajes
        // como leídos por mí mismo NO cambia mi inbox (lo gestiona el FE localmente al disparar
        // markAsRead), así que basta con consultar lastMessage.sentAt > since.
        // Ojo con el orden: leemos primero el último mensaje (1 query) y, solo si la sesión
        // pasa el filtro, leemos el unreadCount (otra query); así evitamos N queries de unread
        // cuando el usuario tiene muchas sesiones inactivas frente a un cursor reciente.
        List<ChatSessionView> enriched = new ArrayList<>(rawSessions.size());
        for (ChatSession session : rawSessions) {
            ChatMessage last = chatMessageRepository.findLastMessageInSession(session).orElse(null);
            if (since != null) {
                if (last == null || !last.getSentAt().isAfter(since)) {
                    continue;
                }
            }
            long unread = chatMessageRepository.countUnreadByChatSessionAndReceiver(session, user.getId());
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
    public ChatPollResult getMessagesPoll(Long chatSessionId, User user, Instant sinceTimestamp) {
        // Capturar serverTime ANTES de las queries: cualquier mensaje que llegue durante el
        // procesamiento queda fuera de esta respuesta pero se entregará en el próximo poll
        // cuando el FE use este serverTime como cursor. Evita carreras y clock skew.
        Instant serverTime = Instant.now();

        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateMessageAccess(chatSession, user);

        List<ChatMessage> messages;
        List<ReadReceipt> readReceipts;
        if (sinceTimestamp == null) {
            // Primera carga: historial completo, sin read-receipts (el cliente lo tiene fresco).
            messages = chatMessageRepository.findByChatSessionOrderBySentAtAsc(chatSession);
            readReceipts = Collections.emptyList();
        } else {
            messages = chatMessageRepository.findByChatSessionAndSentAtAfterOrderBySentAtAsc(chatSession, sinceTimestamp);
            List<ChatMessage> myReadMsgs = chatMessageRepository.findReadReceiptsForSenderSince(
                    chatSession, user.getId(), sinceTimestamp);
            readReceipts = myReadMsgs.stream()
                    .map(m -> new ReadReceipt(m.getId(), m.getReadAt()))
                    .collect(Collectors.toList());
        }

        return new ChatPollResult(messages, readReceipts, serverTime);
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
    public ChatSession getOrCreateSession(User actor, Long patientIdArg, Long practitionerIdArg) {
        if (actor == null) {
            throw new IllegalArgumentException("El actor autenticado no puede ser nulo.");
        }

        // Resolvemos los IDs definitivos en función del rol del actor para impedir suplantación:
        //  - Si el actor es paciente, su propio patientId se toma del JWT; cualquier patientId
        //    pasado por body que no coincida se rechaza con CHAT_PARTICIPANT_MISMATCH.
        //  - Análogo para practicantes.
        //  - El "otro" participante sí viene del body porque no es el actor.
        Long patientId;
        Long practitionerId;
        if (actor.getRole() == Role.ROLE_PATIENT) {
            Patient self = patientRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", actor.getId().toString()));
            patientId = self.getId();
            if (patientIdArg != null && !patientIdArg.equals(patientId)) {
                throw new UnauthorizedOperationException(
                        "El patientId enviado no coincide con el del usuario autenticado.",
                        CODE_PARTICIPANT_MISMATCH);
            }
            if (practitionerIdArg == null) {
                throw new IllegalArgumentException("practitionerId es obligatorio.");
            }
            practitionerId = practitionerIdArg;
        } else if (actor.getRole() == Role.ROLE_PRACTITIONER) {
            Practitioner self = practitionerRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "userId", actor.getId().toString()));
            practitionerId = self.getId();
            if (practitionerIdArg != null && !practitionerIdArg.equals(practitionerId)) {
                throw new UnauthorizedOperationException(
                        "El practitionerId enviado no coincide con el del usuario autenticado.",
                        CODE_PARTICIPANT_MISMATCH);
            }
            if (patientIdArg == null) {
                throw new IllegalArgumentException("patientId es obligatorio.");
            }
            patientId = patientIdArg;
        } else {
            throw new UnauthorizedOperationException(
                    "Solo pacientes y practicantes pueden crear sesiones de chat.",
                    "CHAT_NOT_PARTICIPANT");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId.toString()));
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "id", practitionerId.toString()));

        // Idempotencia: si ya existe, devolvemos la sesión existente sin re-crear.
        var existing = chatSessionRepository.findByPatientAndPractitioner(patient, practitioner);
        if (existing.isPresent()) {
            return existing.get();
        }

        // No existe la sesión: exigimos relación clínica previa (RF27) antes de materializarla.
        boolean hasPriorRelationship = appointmentRepository
                .existsByPatientIdAndPractitionerId(patientId, practitionerId);
        if (!hasPriorRelationship) {
            throw new InvalidBusinessRuleException(
                    "No se puede abrir un chat sin una relación clínica previa entre el paciente y el practicante.",
                    CODE_NO_PRIOR_RELATIONSHIP);
        }

        ChatSession newSession = new ChatSession(patient, practitioner);
        return chatSessionRepository.save(newSession);
    }

    @Override
    public ChatSession blockChatSession(Long chatSessionId, User actor, String reason) {
        ChatSession chatSession = loadSessionOrThrow(chatSessionId);
        chatPolicyService.validateBlockOperation(chatSession, actor);

        // Lógica de negocio en el modelo (Rich Domain): se asegura idempotencia, audit trail y consistencia.
        chatSession.block(actor, actor.getRole(), normalizeReason(reason));
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

    @Override
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        return chatMessageRepository.countTotalUnreadByReceiver(user.getId());
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
