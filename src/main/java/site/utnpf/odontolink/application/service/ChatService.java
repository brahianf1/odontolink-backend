package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.ChatMessageRepository;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.service.ChatPolicyService;

import java.time.Instant;
import java.util.List;

/**
 * Servicio de aplicación para la gestión del chat interno.
 * Implementa el puerto de entrada IChatUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio es el orquestador transaccional de los casos de uso de chat:
 * - CU 6.1: Obtener Lista de Sesiones de Chat
 * - CU 6.2: Enviar un Mensaje (RF26)
 * - CU 6.3: Obtener Mensajes (Polling RESTful)
 *
 * Su responsabilidad principal es coordinar:
 * 1. La carga de entidades de dominio desde los repositorios
 * 2. La delegación de lógica de negocio al servicio de dominio (ChatPolicyService)
 * 3. La persistencia transaccional de los cambios
 *
 * @Transactional asegura que todas las operaciones sean atómicas.
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

    /**
     * Implementa CU 6.1: Obtener Lista de Sesiones de Chat.
     *
     * Orquestación:
     * 1. Determina el rol del usuario
     * 2. Si es paciente: carga el Patient y obtiene sus sesiones
     * 3. Si es practicante: carga el Practitioner y obtiene sus sesiones
     * 4. Retorna la lista de sesiones
     *
     * @param user El usuario autenticado
     * @return Lista de sesiones de chat del usuario
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getMyChatSessions(User user) {
        if (user.getRole() == Role.ROLE_PATIENT) {
            // El usuario es un paciente
            Patient patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", user.getId().toString()));
            return chatSessionRepository.findByPatient(patient);
        } else if (user.getRole() == Role.ROLE_PRACTITIONER) {
            // El usuario es un practicante
            Practitioner practitioner = practitionerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "userId", user.getId().toString()));
            return chatSessionRepository.findByPractitioner(practitioner);
        } else {
            // El usuario no es ni paciente ni practicante
            throw new IllegalArgumentException("Solo los pacientes y practicantes pueden acceder al chat.");
        }
    }

    /**
     * Implementa CU 6.2: Enviar un Mensaje (RF26).
     *
     * Orquestación:
     * 1. Carga la ChatSession desde el repositorio
     * 2. Delega al ChatPolicyService para validar que el sender pertenece a la sesión
     * 3. Crea el ChatMessage con el contenido
     * 4. Persiste el mensaje de forma transaccional
     * 5. Retorna el mensaje guardado
     *
     * @param chatSessionId El ID de la sesión de chat
     * @param content El contenido textual del mensaje
     * @param sender El usuario que envía el mensaje
     * @return El mensaje creado con su ID asignado
     * @throws ResourceNotFoundException si la sesión no existe
     * @throws site.utnpf.odontolink.domain.exception.UnauthorizedOperationException si el usuario no pertenece a la sesión
     */
    @Override
    public ChatMessage sendMessage(Long chatSessionId, String content, User sender) {
        // Cargar la ChatSession desde el repositorio
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", chatSessionId.toString()));

        // Delegar al servicio de dominio para validar la pertenencia (Rulebook)
        chatPolicyService.validateMessageSend(chatSession, sender);

        // Crear el ChatMessage (POJO de dominio)
        ChatMessage newMessage = new ChatMessage(chatSession, sender, content);

        // Persistir el mensaje de forma transaccional
        return chatMessageRepository.save(newMessage);
    }

    /**
     * Implementa CU 6.3: Obtener Mensajes (El Endpoint de "Polling").
     *
     * Orquestación:
     * 1. Carga la ChatSession desde el repositorio
     * 2. Delega al ChatPolicyService para validar el acceso del usuario
     * 3. Si sinceTimestamp es null: retorna todo el historial
     * 4. Si sinceTimestamp no es null: retorna solo mensajes nuevos (polling)
     * 5. Retorna la lista de mensajes ordenada cronológicamente
     *
     * @param chatSessionId El ID de la sesión de chat
     * @param user El usuario que solicita los mensajes
     * @param sinceTimestamp Timestamp opcional para polling (null = primera carga)
     * @return Lista de mensajes ordenados por sentAt ascendente
     * @throws ResourceNotFoundException si la sesión no existe
     * @throws site.utnpf.odontolink.domain.exception.UnauthorizedOperationException si el usuario no tiene acceso
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long chatSessionId, User user, Instant sinceTimestamp) {
        // Cargar la ChatSession desde el repositorio
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", "id", chatSessionId.toString()));

        // Delegar al servicio de dominio para validar el acceso (Rulebook)
        chatPolicyService.validateMessageAccess(chatSession, user);

        // Implementar la lógica de polling
        if (sinceTimestamp == null) {
            // Primera carga: retornar todo el historial
            return chatMessageRepository.findByChatSessionOrderBySentAtAsc(chatSession);
        } else {
            // Polling: retornar solo mensajes nuevos desde el timestamp
            return chatMessageRepository.findByChatSessionAndSentAtAfterOrderBySentAtAsc(chatSession, sinceTimestamp);
        }
    }
}
