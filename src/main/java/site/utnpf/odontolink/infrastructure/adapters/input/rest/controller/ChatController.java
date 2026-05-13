package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.application.port.in.dto.ChatSessionView;
import site.utnpf.odontolink.application.port.in.dto.PagedMessages;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.BlockChatSessionRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.SendMessageRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatSessionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MarkMessagesAsReadResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PagedChatMessagesResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ChatRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para el sistema de chat interno entre paciente y practicante.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - GET    /api/chat/sessions                                 - Inbox con unreadCount y bloqueo (CU 6.1 + CU012)
 * - POST   /api/chat/sessions/{sessionId}/messages            - Enviar mensaje (RF26 - CU 6.2)
 * - GET    /api/chat/sessions/{sessionId}/messages            - Historial / polling / paginado (CU 6.3 + CU012)
 * - POST   /api/chat/sessions/{sessionId}/messages/read       - Marcar mensajes como leídos en bulk (CU012)
 * - POST   /api/chat/sessions/{sessionId}/block               - Bloquear sesión (RF28)
 * - POST   /api/chat/sessions/{sessionId}/unblock             - Desbloquear sesión (RF28 reversible)
 *
 * Seguridad de doble capa:
 *  1. @PreAuthorize corta el tráfico por rol antes de llegar al servicio.
 *  2. ChatPolicyService re-valida pertenencia a la sesión y estado de bloqueo en el dominio.
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Mensajería interna paciente-practicante con bloqueo (RF28), read receipts y unread counts (CU012)")
@SecurityRequirement(name = "Bearer Authentication")
public class ChatController {

    private final IChatUseCase chatUseCase;
    private final AuthenticationFacade authenticationFacade;

    public ChatController(IChatUseCase chatUseCase, AuthenticationFacade authenticationFacade) {
        this.chatUseCase = chatUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Obtiene el inbox del usuario autenticado.
     * Implementa CU 6.1 + CU012 paso 9: lista enriquecida para construir la UI del inbox.
     *
     * Cada sesión incluye:
     * - unreadCount: cantidad de mensajes no leídos para mostrar el badge de notificación
     * - lastMessageAt / lastMessagePreview: para ordenar y mostrar preview tipo WhatsApp
     * - blocked + audit trail: para que la UI muestre el banner correspondiente (RF28)
     *
     * El sistema determina automáticamente si el usuario es paciente o practicante.
     * Las sesiones vienen ordenadas DESC por actividad real (último mensaje o, en su defecto, createdAt).
     *
     * GET /api/chat/sessions
     *
     * Seguridad: Solo PATIENT y PRACTITIONER pueden acceder.
     *
     * @return Lista de sesiones de chat enriquecidas
     */
    @Operation(
            summary = "Obtener inbox de sesiones de chat",
            description = "Devuelve las sesiones del usuario autenticado, enriquecidas con unreadCount, " +
                    "preview del último mensaje y metadatos de bloqueo (RF28). Ordenadas por actividad reciente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de sesiones obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatSessionResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            [
                                              {
                                                "id": 42,
                                                "patientId": 15,
                                                "patientName": "Carlos Rodríguez",
                                                "practitionerId": 8,
                                                "practitionerName": "Ana Martínez",
                                                "createdAt": "2025-11-10T10:00:00Z",
                                                "unreadCount": 3,
                                                "lastMessageAt": "2026-05-13T09:42:11Z",
                                                "lastMessagePreview": "Hola doctora, ¿podemos confirmar el turno del jueves?",
                                                "blocked": false,
                                                "blockedByUserId": null,
                                                "blockedByRole": null,
                                                "blockedAt": null,
                                                "blockReason": null
                                              },
                                              {
                                                "id": 17,
                                                "patientId": 22,
                                                "patientName": "Sofía López",
                                                "practitionerId": 8,
                                                "practitionerName": "Ana Martínez",
                                                "createdAt": "2025-09-02T12:30:00Z",
                                                "unreadCount": 0,
                                                "lastMessageAt": "2026-04-01T15:00:00Z",
                                                "lastMessagePreview": "Gracias por la atención.",
                                                "blocked": true,
                                                "blockedByUserId": 8,
                                                "blockedByRole": "ROLE_PRACTITIONER",
                                                "blockedAt": "2026-04-02T08:00:00Z",
                                                "blockReason": "Uso indebido del canal de chat"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado (solo PATIENT o PRACTITIONER)", content = @Content)
    })
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<List<ChatSessionResponseDTO>> getMyChatSessions() {

        // Obtener el usuario autenticado
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso: trae sesiones + unreadCount + último mensaje
        List<ChatSessionView> views = chatUseCase.getMyChatSessions(authenticatedUser);

        // Mapear a DTOs de respuesta
        List<ChatSessionResponseDTO> response = views.stream()
                .map(ChatRestMapper::toChatSessionResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Envía un nuevo mensaje a una sesión de chat existente.
     * Implementa RF26 - CU 6.2: Enviar un Mensaje.
     *
     * El sistema valida automáticamente:
     * - Que el usuario es un participante legítimo de la sesión.
     * - Que la sesión no esté bloqueada (si lo está, el paciente queda silenciado pero el
     *   practicante conserva voz para seguir documentando — RF28).
     *
     * POST /api/chat/sessions/{sessionId}/messages
     *
     * Seguridad: Solo PATIENT y PRACTITIONER pueden acceder. El bloqueo RF28 se enforce a
     * nivel de dominio: aunque el frontend permita enviar, el backend devolverá 403.
     *
     * @param sessionId ID de la sesión de chat (path)
     * @param requestDTO DTO con el contenido del mensaje (body)
     * @return El mensaje creado con status 201 Created
     */
    @Operation(
            summary = "Enviar un mensaje a una sesión de chat",
            description = "Crea un nuevo mensaje en la sesión indicada. El sender se obtiene del JWT. " +
                    "Si la sesión está bloqueada (RF28), el paciente recibirá 403; el practicante puede seguir escribiendo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Mensaje enviado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 1024,
                                              "chatSessionId": 42,
                                              "senderId": 15,
                                              "senderName": "Carlos Rodríguez",
                                              "content": "Hola doctora, ¿podemos confirmar el turno del jueves?",
                                              "sentAt": "2026-05-13T09:42:11Z",
                                              "readAt": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Contenido vacío o supera 2000 caracteres", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuario no pertenece a la sesión o sesión bloqueada (RF28)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Contenido del mensaje a enviar",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "content": "Hola doctora, ¿podemos confirmar el turno del jueves?"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @Parameter(description = "ID de la sesión de chat", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequestDTO requestDTO) {

        // Obtener el usuario autenticado (sender)
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al servicio de aplicación (valida pertenencia + bloqueo)
        ChatMessage createdMessage = chatUseCase.sendMessage(sessionId, requestDTO.getContent(), authenticatedUser);

        // Mapear a DTO de respuesta
        ChatMessageResponseDTO responseDTO = ChatRestMapper.toChatMessageResponseDTO(createdMessage);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * Obtiene los mensajes de una sesión de chat. Soporta tres modos de consumo:
     *
     * 1) Sin parámetros: devuelve TODO el historial en orden ASC (cronológico).
     *    Recomendado solo si la conversación es corta. Para chats largos usar paginación.
     *
     * 2) ?since={ISO-8601}: modo polling RESTful. Devuelve solo los mensajes con
     *    sentAt > since. El frontend debe llamar periódicamente (5-10s) pasando el sentAt
     *    del último mensaje recibido. Implementa CU 6.3.
     *
     * 3) ?page=N&size=M: paginación DESC para "scroll infinito hacia arriba" (CU012).
     *    Devuelve un envoltorio PagedChatMessagesResponseDTO con metadatos de página.
     *    Los mensajes vienen ordenados DESC (más reciente primero) — el frontend los renderiza
     *    en orden inverso al cargar páginas anteriores. Tamaño máximo de página: 200.
     *
     * Precedencia: si se envían 'page'/'size' Y 'since', tiene precedencia la paginación.
     *
     * GET /api/chat/sessions/{sessionId}/messages
     *
     * Seguridad: Solo PATIENT y PRACTITIONER que pertenezcan a la sesión.
     *
     * @param sessionId ID de la sesión (path)
     * @param since Timestamp ISO-8601 opcional (query) — modo polling
     * @param page Número de página opcional (query) — modo paginado, base 0
     * @param size Tamaño de página opcional (query) — entre 1 y 200, default 50
     * @return Lista de mensajes (modos 1/2) o PagedChatMessagesResponseDTO (modo 3)
     */
    @Operation(
            summary = "Obtener mensajes de una sesión",
            description = "Tres modos: (a) sin params → historial completo ASC; " +
                    "(b) ?since=ISO8601 → polling de nuevos mensajes; " +
                    "(c) ?page=N&size=M → paginación DESC para scroll-up (CU012). " +
                    "La pertenencia a la sesión se valida en el dominio."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Mensajes obtenidos exitosamente. Estructura depende del modo: " +
                            "lista plana para modos full/polling, envoltorio paginado para modo page/size.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Modo full / polling (lista de mensajes)",
                                            value = """
                                                    [
                                                      {
                                                        "id": 1024,
                                                        "chatSessionId": 42,
                                                        "senderId": 15,
                                                        "senderName": "Carlos Rodríguez",
                                                        "content": "Hola doctora",
                                                        "sentAt": "2026-05-13T09:42:11Z",
                                                        "readAt": "2026-05-13T09:43:01Z"
                                                      },
                                                      {
                                                        "id": 1025,
                                                        "chatSessionId": 42,
                                                        "senderId": 8,
                                                        "senderName": "Ana Martínez",
                                                        "content": "Hola Carlos, sí, confirmado.",
                                                        "sentAt": "2026-05-13T09:45:02Z",
                                                        "readAt": null
                                                      }
                                                    ]
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Modo paginado (page/size)",
                                            value = """
                                                    {
                                                      "messages": [
                                                        {
                                                          "id": 1025,
                                                          "chatSessionId": 42,
                                                          "senderId": 8,
                                                          "senderName": "Ana Martínez",
                                                          "content": "Hola Carlos, sí, confirmado.",
                                                          "sentAt": "2026-05-13T09:45:02Z",
                                                          "readAt": null
                                                        }
                                                      ],
                                                      "page": 0,
                                                      "size": 50,
                                                      "totalElements": 312,
                                                      "totalPages": 7,
                                                      "last": false
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "page < 0 o size fuera de [1, 200]", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuario no pertenece a la sesión", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content)
    })
    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<?> getMessages(
            @Parameter(description = "ID de la sesión de chat", required = true)
            @PathVariable Long sessionId,
            @Parameter(description = "Timestamp ISO-8601 para modo polling (solo mensajes posteriores)", example = "2026-05-13T09:42:11Z")
            @RequestParam(required = false) Instant since,
            @Parameter(description = "Número de página (base 0) para modo paginado", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamaño de página entre 1 y 200 (default 50)", example = "50")
            @RequestParam(required = false) Integer size) {

        // Obtener el usuario autenticado
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Modo paginado tiene precedencia sobre 'since' (paginación es para historial)
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 50;
            PagedMessages pageResult = chatUseCase.getMessagesPaged(sessionId, authenticatedUser, p, s);
            return ResponseEntity.ok(ChatRestMapper.toPagedChatMessagesResponseDTO(pageResult));
        }

        // Modo full o polling (según presencia de 'since')
        List<ChatMessage> messages = chatUseCase.getMessages(sessionId, authenticatedUser, since);
        List<ChatMessageResponseDTO> response = messages.stream()
                .map(ChatRestMapper::toChatMessageResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Marca como leídos en bulk todos los mensajes pendientes de la contraparte.
     * Implementa CU012 - Read Receipts.
     *
     * Comportamiento:
     * - Marca TODOS los mensajes de la sesión cuyo sender NO sea el usuario autenticado y
     *   cuyo readAt sea null.
     * - La marca se hace con un único UPDATE SQL → seguro para conversaciones con cientos
     *   de mensajes pendientes (evita N+1).
     * - Idempotente: una segunda llamada simplemente marcará 0 mensajes.
     *
     * Caso de uso típico: el frontend llama a este endpoint cuando el receptor abre la
     * conversación, en paralelo con el GET de mensajes.
     *
     * POST /api/chat/sessions/{sessionId}/messages/read
     *
     * Seguridad: PATIENT o PRACTITIONER, debe ser participante de la sesión.
     *
     * @param sessionId ID de la sesión de chat
     * @return DTO con el conteo de mensajes marcados y el timestamp aplicado
     */
    @Operation(
            summary = "Marcar mensajes como leídos (bulk)",
            description = "Marca como leídos todos los mensajes no leídos enviados por la contraparte " +
                    "en una única operación SQL. Idempotente. Solo el receptor puede invocarlo " +
                    "(la pertenencia se valida en el dominio)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Operación ejecutada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MarkMessagesAsReadResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "chatSessionId": 42,
                                              "messagesMarked": 3,
                                              "readAt": "2026-05-13T09:43:01Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Usuario no pertenece a la sesión", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content)
    })
    @PostMapping("/sessions/{sessionId}/messages/read")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<MarkMessagesAsReadResponseDTO> markMessagesAsRead(
            @Parameter(description = "ID de la sesión de chat", required = true)
            @PathVariable Long sessionId) {

        // Obtener el usuario autenticado (receptor)
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Capturamos el timestamp en el controlador para devolverlo exactamente igual al cliente
        Instant readAt = Instant.now();
        int marked = chatUseCase.markMessagesAsRead(sessionId, authenticatedUser);

        return ResponseEntity.ok(new MarkMessagesAsReadResponseDTO(sessionId, marked, readAt));
    }

    /**
     * Bloquea la sesión de chat impidiendo que el paciente envíe nuevos mensajes.
     * Implementa RF28 - Historia #17: Bloqueo del paciente por parte del practicante.
     *
     * Reglas de negocio:
     * - Solo el PRACTITIONER de la sesión puede bloquear (no cualquier practicante).
     * - El bloqueo deja rastro auditable: blockedByUser, blockedByRole, blockedAt y blockReason.
     * - Una vez bloqueada, los mensajes enviados por el paciente reciben 403.
     * - El practicante conserva voz: puede seguir documentando aún sobre una sesión bloqueada
     *   (decisión clínica para no perder capacidad de registro).
     * - El motivo es opcional. Si se envía, se persiste (máx. 500 caracteres).
     *
     * POST /api/chat/sessions/{sessionId}/block
     *
     * Seguridad: Solo PRACTITIONER que sea participante de la sesión.
     *
     * @param sessionId ID de la sesión a bloquear
     * @param requestDTO DTO opcional con el motivo del bloqueo
     * @return La sesión actualizada con el rastro de bloqueo
     */
    @Operation(
            summary = "Bloquear sesión de chat (RF28)",
            description = "El practicante de la sesión bloquea al paciente: deja rastro auditable " +
                    "(blockedByUser, blockedByRole, blockedAt, blockReason) y silencia al paciente. " +
                    "El practicante puede seguir escribiendo. Operación reversible vía /unblock."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión bloqueada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatSessionResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 42,
                                              "patientId": 15,
                                              "patientName": "Carlos Rodríguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martínez",
                                              "createdAt": "2025-11-10T10:00:00Z",
                                              "unreadCount": 0,
                                              "lastMessageAt": null,
                                              "lastMessagePreview": null,
                                              "blocked": true,
                                              "blockedByUserId": 8,
                                              "blockedByRole": "ROLE_PRACTITIONER",
                                              "blockedAt": "2026-05-13T10:00:00Z",
                                              "blockReason": "Uso indebido del canal de chat"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Motivo supera 500 caracteres", content = @Content),
            @ApiResponse(responseCode = "403", description = "Solo el practicante de la sesión puede bloquear", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "La sesión ya estaba bloqueada", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Motivo opcional del bloqueo (puede omitirse el body entero)",
            required = false,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "reason": "Uso indebido del canal de chat"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/sessions/{sessionId}/block")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<ChatSessionResponseDTO> blockChatSession(
            @Parameter(description = "ID de la sesión de chat a bloquear", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody(required = false) BlockChatSessionRequestDTO requestDTO) {

        // Obtener el usuario autenticado (practicante)
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // El body es opcional: si no viene, reason queda null
        String reason = requestDTO != null ? requestDTO.getReason() : null;

        // Delegar al caso de uso: valida rol + pertenencia + idempotencia
        ChatSession updated = chatUseCase.blockChatSession(sessionId, authenticatedUser, reason);

        return ResponseEntity.ok(ChatRestMapper.toChatSessionResponseDTO(updated));
    }

    /**
     * Desbloquea la sesión de chat previamente bloqueada (reversibilidad de RF28).
     *
     * Reglas:
     * - Solo el PRACTITIONER de la sesión puede desbloquear.
     * - El audit trail original (quién bloqueó y cuándo) se borra al desbloquear, porque ya no
     *   hay bloqueo activo. Si se necesitara historial de bloqueos previos a futuro, sería un
     *   feature aparte (event sourcing / tabla de auditoría dedicada).
     *
     * POST /api/chat/sessions/{sessionId}/unblock
     *
     * Seguridad: Solo PRACTITIONER que sea participante de la sesión.
     *
     * @param sessionId ID de la sesión a desbloquear
     * @return La sesión actualizada sin rastro de bloqueo
     */
    @Operation(
            summary = "Desbloquear sesión de chat (RF28 reversible)",
            description = "Revierte el bloqueo previamente aplicado. Solo el practicante de la sesión " +
                    "puede invocarlo. Limpia los campos de auditoría del bloqueo activo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión desbloqueada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatSessionResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 42,
                                              "patientId": 15,
                                              "patientName": "Carlos Rodríguez",
                                              "practitionerId": 8,
                                              "practitionerName": "Ana Martínez",
                                              "createdAt": "2025-11-10T10:00:00Z",
                                              "unreadCount": 0,
                                              "lastMessageAt": null,
                                              "lastMessagePreview": null,
                                              "blocked": false,
                                              "blockedByUserId": null,
                                              "blockedByRole": null,
                                              "blockedAt": null,
                                              "blockReason": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Solo el practicante de la sesión puede desbloquear", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "La sesión no estaba bloqueada", content = @Content)
    })
    @PostMapping("/sessions/{sessionId}/unblock")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<ChatSessionResponseDTO> unblockChatSession(
            @Parameter(description = "ID de la sesión de chat a desbloquear", required = true)
            @PathVariable Long sessionId) {

        // Obtener el usuario autenticado (practicante)
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Delegar al caso de uso: valida rol + pertenencia + idempotencia
        ChatSession updated = chatUseCase.unblockChatSession(sessionId, authenticatedUser);

        return ResponseEntity.ok(ChatRestMapper.toChatSessionResponseDTO(updated));
    }
}
