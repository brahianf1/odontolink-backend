package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.SendMessageRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatSessionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ChatRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para el sistema de chat interno.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - GET /api/chat/sessions: Obtener lista de sesiones de chat (CU 6.1)
 * - POST /api/chat/sessions/{sessionId}/messages: Enviar mensaje (CU 6.2)
 * - GET /api/chat/sessions/{sessionId}/messages: Obtener mensajes con polling (CU 6.3)
 *
 * Todos los endpoints están protegidos con @PreAuthorize según el rol requerido.
 * Solo pacientes y practicantes pueden acceder al chat.
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final IChatUseCase chatUseCase;
    private final AuthenticationFacade authenticationFacade;

    public ChatController(IChatUseCase chatUseCase,
                         AuthenticationFacade authenticationFacade) {
        this.chatUseCase = chatUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Endpoint: GET /api/chat/sessions
     * Implementa CU 6.1: Obtener Lista de Sesiones de Chat (El "Inbox").
     *
     * Retorna todas las sesiones de chat del usuario autenticado.
     * El sistema determina automáticamente si el usuario es paciente o practicante.
     *
     * GET /api/chat/sessions
     *
     * Seguridad: Solo PATIENT y PRACTITIONER pueden acceder
     *
     * @return Lista de sesiones de chat del usuario
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<List<ChatSessionResponseDTO>> getMyChatSessions() {

        // Obtener el usuario autenticado
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso (servicio de aplicación)
        List<ChatSession> sessions = chatUseCase.getMyChatSessions(authenticatedUser);

        // Convertir a DTOs de respuesta
        List<ChatSessionResponseDTO> responseDTOs = sessions.stream()
                .map(ChatRestMapper::toChatSessionResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * Endpoint: POST /api/chat/sessions/{sessionId}/messages
     * Implementa CU 6.2: Enviar un Mensaje (RF26).
     *
     * Envía un nuevo mensaje a una sesión de chat existente.
     * El sistema valida automáticamente que el usuario pertenece a la sesión.
     *
     * POST /api/chat/sessions/{sessionId}/messages
     *
     * Seguridad: Solo PATIENT y PRACTITIONER pueden acceder
     *
     * @param sessionId El ID de la sesión de chat (path parameter)
     * @param requestDTO El DTO con el contenido del mensaje (request body)
     * @return El mensaje creado con status 201 Created
     */
    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequestDTO requestDTO) {

        // Obtener el usuario autenticado (sender)
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al servicio de aplicación
        ChatMessage createdMessage = chatUseCase.sendMessage(
                sessionId,
                requestDTO.getContent(),
                authenticatedUser
        );

        // Mapear a DTO de respuesta
        ChatMessageResponseDTO responseDTO = ChatRestMapper.toChatMessageResponseDTO(createdMessage);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * Endpoint: GET /api/chat/sessions/{sessionId}/messages
     * Implementa CU 6.3: Obtener Mensajes (El Endpoint de "Polling").
     *
     * Obtiene los mensajes de una sesión de chat con soporte para polling.
     * - Si 'since' no se proporciona: retorna todo el historial
     * - Si 'since' se proporciona: retorna solo mensajes nuevos desde ese timestamp
     *
     * El frontend debe llamar a este endpoint periódicamente (cada 5-10 segundos)
     * pasando el 'sentAt' del último mensaje recibido para implementar el polling.
     *
     * GET /api/chat/sessions/{sessionId}/messages?since={timestamp}
     *
     * Seguridad: Solo PATIENT y PRACTITIONER pueden acceder
     *
     * @param sessionId El ID de la sesión de chat (path parameter)
     * @param since Timestamp opcional para polling (query parameter)
     * @return Lista de mensajes ordenados cronológicamente
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Instant since) {

        // Obtener el usuario autenticado
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al servicio de aplicación
        List<ChatMessage> messages = chatUseCase.getMessages(sessionId, authenticatedUser, since);

        // Mapear a DTOs de respuesta
        List<ChatMessageResponseDTO> responseDTOs = messages.stream()
                .map(ChatRestMapper::toChatMessageResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }
}
