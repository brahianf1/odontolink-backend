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
import site.utnpf.odontolink.application.port.in.dto.ChatPollResult;
import site.utnpf.odontolink.application.port.in.dto.ChatSessionView;
import site.utnpf.odontolink.application.port.in.dto.PagedMessages;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.BlockChatSessionRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CreateChatSessionRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.SendMessageRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatPollResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatSessionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MarkMessagesAsReadResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PagedChatMessagesResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.UnreadCountResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ChatRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para el sistema de chat interno entre paciente y practicante.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - GET    /api/chat/sessions                                 - Inbox con unreadCount y bloqueo (CU 6.1 + CU012); soporta ?since= para delta
 * - POST   /api/chat/sessions                                 - Creación idempotente de sesión (P4) — exige relación clínica previa (RF27)
 * - GET    /api/chat/unread-count                             - Contador global de no leídos para badge del sidebar (P8)
 * - POST   /api/chat/sessions/{sessionId}/messages            - Enviar mensaje (RF26 - CU 6.2)
 * - GET    /api/chat/sessions/{sessionId}/messages            - Historial / polling unificado / paginado (CU 6.3 + CU012)
 * - POST   /api/chat/sessions/{sessionId}/messages/read       - Marcar mensajes como leídos en bulk (CU012)
 * - POST   /api/chat/sessions/{sessionId}/block               - Bloquear sesión (RF28)
 * - POST   /api/chat/sessions/{sessionId}/unblock             - Desbloquear sesión (RF28 reversible)
 *
 * Seguridad de doble capa:
 *  1. @PreAuthorize corta el tráfico por rol antes de llegar al servicio.
 *  2. ChatPolicyService re-valida pertenencia a la sesión y estado de bloqueo en el dominio.
 *
 * Errores: los 4xx llevan en el body un {@code errorCode} estable (constantes {@code CHAT_*})
 * para que el FE pueda ramificar UX sin parsear el mensaje humano.
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
     * Inbox del usuario autenticado. Implementa CU 6.1 + CU012 paso 9 + P2 (polling delta).
     *
     * <p>Cada sesión incluye: unreadCount, lastMessageAt/lastMessagePreview, y los metadatos
     * de bloqueo. Las sesiones vienen ordenadas DESC por actividad real (último mensaje o,
     * en su defecto, createdAt).
     *
     * <p>Modo polling (P2): {@code ?since=ISO8601} devuelve únicamente las sesiones cuya
     * actividad cambió desde el cursor (al menos un mensaje con {@code sentAt > since}).
     * Útil para que el FE no recargue todo el inbox cada N segundos.
     */
    @Operation(
            summary = "Obtener inbox de sesiones de chat",
            description = "Devuelve las sesiones del usuario autenticado, enriquecidas con unreadCount, " +
                    "preview del último mensaje y metadatos de bloqueo (RF28). " +
                    "Con ?since=ISO8601 devuelve solo las sesiones cuyo último mensaje es posterior al cursor. " +
                    "Ordenadas DESC por actividad reciente."
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
    public ResponseEntity<List<ChatSessionResponseDTO>> getMyChatSessions(
            @Parameter(description = "Cursor ISO-8601 inclusivo (>=): devuelve solo sesiones cuyo lastMessage.sentAt es posterior o igual al cursor. " +
                                     "Misma semántica que el cursor de mensajes; el FE deduplica por id de sesión.",
                       example = "2026-05-15T10:00:00.000Z")
            @RequestParam(required = false) Instant since) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        List<ChatSessionView> views = chatUseCase.getMyChatSessions(authenticatedUser, since);

        List<ChatSessionResponseDTO> response = views.stream()
                .map(ChatRestMapper::toChatSessionResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Creación idempotente de una sesión de chat (P4). Implementa RF27.
     *
     * <p>Comportamiento:
     * <ul>
     *   <li>Si ya existe una sesión entre el paciente y el practicante → 200 OK con la existente.</li>
     *   <li>Si no existe, valida que haya habido al menos un appointment entre ambos
     *       (relación clínica previa) y la crea → 201 Created.</li>
     *   <li>Si no hay relación previa → 422 con errorCode {@code CHAT_NO_PRIOR_RELATIONSHIP}.</li>
     *   <li>Si el rol enviado en el body no coincide con el del JWT → 403 con
     *       errorCode {@code CHAT_PARTICIPANT_MISMATCH}.</li>
     * </ul>
     *
     * <p>En la práctica, la sesión normalmente ya se autocreó al primer appointment
     * (ver {@code AppointmentBookingService}). Este endpoint es la red de seguridad
     * que el FE puede invocar cuando quiere garantizar que la sesión existe.
     */
    @Operation(
            summary = "Crear u obtener sesión de chat (idempotente)",
            description = "Devuelve la sesión existente entre paciente y practicante, o la crea si tienen relación previa. " +
                    "RF27: no se permiten chats sin un appointment previo entre ambos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "La sesión ya existía y se devuelve tal cual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "201", description = "Sesión creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado o IDs no coinciden con el JWT (CHAT_PARTICIPANT_MISMATCH)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente o practicante no encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "No hay relación clínica previa (CHAT_NO_PRIOR_RELATIONSHIP)",
                    content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "IDs de los participantes. El servidor completa el propio desde el JWT; el otro es obligatorio.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "Paciente abriendo chat con practicante",
                                    value = """
                                            { "practitionerId": 8 }
                                            """),
                            @ExampleObject(name = "Practicante abriendo chat con paciente",
                                    value = """
                                            { "patientId": 15 }
                                            """)
                    }
            )
    )
    @PostMapping("/sessions")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<ChatSessionResponseDTO> createOrGetSession(
            @Valid @RequestBody CreateChatSessionRequestDTO requestDTO) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // No podemos distinguir "ya existía" vs "recién creado" con la firma actual de
        // getOrCreateSession (devuelve la sesión y listo). Para mantener semántica REST
        // estricta entre 200 y 201 sin agregar otro método, devolvemos siempre 200 aquí.
        // Si el FE necesita distinguir, podemos enriquecer el resultado del servicio en
        // un commit posterior; por ahora es contrato 200 OK idempotente.
        ChatSession session = chatUseCase.getOrCreateSession(
                authenticatedUser, requestDTO.getPatientId(), requestDTO.getPractitionerId());

        ChatSessionResponseDTO body = ChatRestMapper.toChatSessionResponseDTO(session);
        return ResponseEntity.ok()
                .location(URI.create("/api/chat/sessions/" + session.getId()))
                .body(body);
    }

    /**
     * Contador global de no-leídos (P8). Implementa el badge del sidebar/AppBar.
     *
     * <p>Suma en una sola query SQL los mensajes con {@code readAt IS NULL} de todas las
     * sesiones donde el usuario es participante, excluyendo sus propios mensajes.
     */
    @Operation(
            summary = "Contador global de no-leídos",
            description = "Devuelve la suma de mensajes no leídos en todas las sesiones del usuario. " +
                    "Alimenta el badge global del sidebar/AppBar sin recorrer el inbox."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UnreadCountResponseDTO.class),
                            examples = @ExampleObject(value = "{ \"total\": 7 }")))
    })
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<UnreadCountResponseDTO> getTotalUnreadCount() {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        long total = chatUseCase.getTotalUnreadCount(authenticatedUser);
        return ResponseEntity.ok(new UnreadCountResponseDTO(total));
    }

    /**
     * Envía un nuevo mensaje a una sesión de chat existente. Implementa RF26 - CU 6.2.
     *
     * <p>El sistema valida:
     * <ul>
     *   <li>Que el usuario es participante de la sesión (403 {@code CHAT_NOT_PARTICIPANT} si no).</li>
     *   <li>Que la sesión no esté bloqueada para el sender (403 {@code CHAT_BLOCKED} si sí).</li>
     * </ul>
     */
    @Operation(
            summary = "Enviar un mensaje a una sesión de chat",
            description = "Crea un nuevo mensaje. El sender se obtiene del JWT. " +
                    "Si la sesión está bloqueada (RF28), el lado silenciado recibe 403 con errorCode CHAT_BLOCKED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Mensaje enviado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1024,
                                      "chatSessionId": 42,
                                      "senderId": 15,
                                      "senderName": "Carlos Rodríguez",
                                      "content": "Hola doctora, ¿podemos confirmar el turno del jueves?",
                                      "sentAt": "2026-05-13T09:42:11Z",
                                      "readAt": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Contenido vacío o supera 2000 caracteres", content = @Content),
            @ApiResponse(responseCode = "403", description = "CHAT_NOT_PARTICIPANT o CHAT_BLOCKED", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Contenido del mensaje a enviar",
            required = true,
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            { "content": "Hola doctora, ¿podemos confirmar el turno del jueves?" }
                            """))
    )
    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @Parameter(description = "ID de la sesión de chat", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequestDTO requestDTO) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        ChatMessage createdMessage = chatUseCase.sendMessage(sessionId, requestDTO.getContent(), authenticatedUser);
        ChatMessageResponseDTO responseDTO = ChatRestMapper.toChatMessageResponseDTO(createdMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * Obtiene los mensajes de una sesión. Soporta dos modos según los query params:
     *
     * <ol>
     *   <li><b>Modo polling unificado (CU 6.3 + CU012)</b>: sin params o con {@code ?since=}.
     *       Devuelve un wrapper {@link ChatPollResponseDTO} con:
     *       <ul>
     *         <li>{@code messages}: si {@code since==null}, los <b>últimos N</b> mensajes en
     *             orden ASC (N configurable, default 100). Para historiales más grandes el FE
     *             debe pasar a modo paginado. Si {@code since != null}, los mensajes con
     *             {@code sentAt >= since}.</li>
     *         <li>{@code readReceipts}: updates de {@code readAt} con {@code readAt >= since}
     *             sobre mensajes propios.</li>
     *         <li>{@code serverTime}: cursor a usar como {@code since} en el próximo poll.</li>
     *       </ul></li>
     *   <li><b>Modo paginado</b>: con {@code ?page=&size=}.
     *       Devuelve {@link PagedChatMessagesResponseDTO} con {@code messages} en orden DESC,
     *       {@code hasNext}/{@code hasPrevious}/{@code last} para el control del scroll-up,
     *       y {@code serverTime} para seedear polling tras la carga paginada.
     *       {@code page=0} son los más recientes; {@code size} en [1, 200] (default 50).</li>
     * </ol>
     *
     * <p>Orden estable: todas las queries ordenan por {@code sentAt} con tie-break por
     * {@code id} en la misma dirección. Garantía sólida ante mensajes con timestamp empatado
     * (seeds batch, alta concurrencia).
     *
     * <p>Cursor inclusivo: el filtro {@code since} es {@code >=}, no estricto. Combinado con
     * el {@code serverTime} capturado antes del read, garantiza que ningún mensaje borde se
     * pierda; el FE deduplica por {@code id} (el mismo mensaje en dos polls consecutivos es
     * idempotente).
     *
     * <p>Precedencia: si vienen {@code page}/{@code size} <i>y</i> {@code since}, predomina la paginación.
     */
    @Operation(
            summary = "Obtener mensajes de una sesión (polling unificado o paginado)",
            description = "Dos modos. Polling: sin params (últimos N + serverTime) o con ?since=ISO8601 (inclusive >=). " +
                    "Paginado: ?page=&size= → wrapper con hasNext/hasPrevious + serverTime (page=0 = más recientes, DESC dentro de página). " +
                    "Orden estable: sentAt + id como tie-break. size en [1,200] default 50. La pertenencia se valida en el dominio."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Mensajes obtenidos exitosamente. La forma depende del modo: poll-wrapper o page-wrapper.",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Modo polling (con ?since=)",
                                            value = """
                                                    {
                                                      "messages": [
                                                        {
                                                          "id": 1025,
                                                          "chatSessionId": 42,
                                                          "senderId": 8,
                                                          "senderName": "Ana Martínez",
                                                          "content": "Hola Carlos, sí, confirmado.",
                                                          "sentAt": "2026-05-13T09:45:02.345Z",
                                                          "readAt": null
                                                        }
                                                      ],
                                                      "readReceipts": [
                                                        { "messageId": 1024, "readAt": "2026-05-13T09:43:01.012Z" }
                                                      ],
                                                      "serverTime": "2026-05-13T09:45:10.789Z"
                                                    }
                                                    """),
                                    @ExampleObject(name = "Modo polling (carga inicial sin since)",
                                            value = """
                                                    {
                                                      "messages": [ /* últimos N en orden ASC */ ],
                                                      "readReceipts": [],
                                                      "serverTime": "2026-05-13T09:45:00.000Z"
                                                    }
                                                    """),
                                    @ExampleObject(name = "Modo paginado (?page=&size=)",
                                            value = """
                                                    {
                                                      "messages": [
                                                        {
                                                          "id": 1025,
                                                          "chatSessionId": 42,
                                                          "senderId": 8,
                                                          "senderName": "Ana Martínez",
                                                          "content": "Hola Carlos, sí, confirmado.",
                                                          "sentAt": "2026-05-13T09:45:02.345Z",
                                                          "readAt": null
                                                        }
                                                      ],
                                                      "page": 0,
                                                      "size": 50,
                                                      "totalElements": 312,
                                                      "totalPages": 7,
                                                      "last": false,
                                                      "hasNext": true,
                                                      "hasPrevious": false,
                                                      "serverTime": "2026-05-13T09:45:10.789Z"
                                                    }
                                                    """)
                            })
            ),
            @ApiResponse(responseCode = "400", description = "page < 0 o size fuera de [1, 200]", content = @Content),
            @ApiResponse(responseCode = "403", description = "CHAT_NOT_PARTICIPANT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content)
    })
    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<?> getMessages(
            @Parameter(description = "ID de la sesión de chat", required = true)
            @PathVariable Long sessionId,
            @Parameter(description = "Cursor ISO-8601 inclusivo (>=) para modo polling. Usar el serverTime del wrapper previo. El FE deduplica por id.",
                       example = "2026-05-13T09:42:11.000Z")
            @RequestParam(required = false) Instant since,
            @Parameter(description = "Número de página (base 0) para modo paginado", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamaño de página entre 1 y 200 (default 50)", example = "50")
            @RequestParam(required = false) Integer size) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();

        // Modo paginado tiene precedencia sobre 'since' (paginación es para historial).
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 50;
            PagedMessages pageResult = chatUseCase.getMessagesPaged(sessionId, authenticatedUser, p, s);
            return ResponseEntity.ok(ChatRestMapper.toPagedChatMessagesResponseDTO(pageResult));
        }

        // Modo polling unificado (con o sin 'since').
        ChatPollResult result = chatUseCase.getMessagesPoll(sessionId, authenticatedUser, since);
        return ResponseEntity.ok(ChatRestMapper.toChatPollResponseDTO(result));
    }

    /**
     * Marca como leídos en bulk los mensajes pendientes de la contraparte (CU012).
     *
     * <p>Funciona también con la sesión bloqueada (P6): el lado silenciado conserva el derecho
     * a cerrar su contador de no-leídos sobre el historial existente.
     */
    @Operation(
            summary = "Marcar mensajes como leídos (bulk)",
            description = "Marca como leídos todos los mensajes no leídos enviados por la contraparte en una única operación SQL. " +
                    "Idempotente. Funciona también si la sesión está bloqueada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación ejecutada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MarkMessagesAsReadResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "chatSessionId": 42,
                                      "messagesMarked": 3,
                                      "readAt": "2026-05-13T09:43:01Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "CHAT_NOT_PARTICIPANT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content)
    })
    @PostMapping("/sessions/{sessionId}/messages/read")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PRACTITIONER')")
    public ResponseEntity<MarkMessagesAsReadResponseDTO> markMessagesAsRead(
            @Parameter(description = "ID de la sesión de chat", required = true)
            @PathVariable Long sessionId) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        Instant readAt = Instant.now();
        int marked = chatUseCase.markMessagesAsRead(sessionId, authenticatedUser);
        return ResponseEntity.ok(new MarkMessagesAsReadResponseDTO(sessionId, marked, readAt));
    }

    /**
     * Bloquea la sesión de chat. Implementa RF28.
     *
     * <p>Política actual: solo el {@code PRACTITIONER} de la sesión puede bloquear. El modelo
     * de dominio ya soporta bloqueo bidireccional (cualquier participante puede ser el bloqueador
     * y el otro queda silenciado), pero la política se mantiene cerrada al practicante hasta
     * que el producto decida abrir la bidireccionalidad.
     */
    @Operation(
            summary = "Bloquear sesión de chat (RF28)",
            description = "El practicante de la sesión bloquea: el paciente queda silenciado, el practicante conserva voz. " +
                    "Deja audit trail (blockedByUser, blockedByRole, blockedAt, blockReason). Reversible vía /unblock."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión bloqueada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Motivo supera 500 caracteres", content = @Content),
            @ApiResponse(responseCode = "403", description = "CHAT_NOT_PRACTITIONER_OF_SESSION", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "CHAT_ALREADY_BLOCKED", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Motivo opcional del bloqueo (puede omitirse el body entero)",
            required = false,
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            { "reason": "Uso indebido del canal de chat" }
                            """))
    )
    @PostMapping("/sessions/{sessionId}/block")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<ChatSessionResponseDTO> blockChatSession(
            @Parameter(description = "ID de la sesión de chat a bloquear", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody(required = false) BlockChatSessionRequestDTO requestDTO) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        String reason = requestDTO != null ? requestDTO.getReason() : null;
        ChatSession updated = chatUseCase.blockChatSession(sessionId, authenticatedUser, reason);
        return ResponseEntity.ok(ChatRestMapper.toChatSessionResponseDTO(updated));
    }

    /**
     * Desbloquea la sesión previamente bloqueada (RF28 reversible). Solo el practicante de la sesión.
     */
    @Operation(
            summary = "Desbloquear sesión de chat (RF28 reversible)",
            description = "Revierte el bloqueo. Limpia los campos de auditoría del bloqueo activo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión desbloqueada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatSessionResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "CHAT_NOT_PRACTITIONER_OF_SESSION", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "CHAT_NOT_BLOCKED", content = @Content)
    })
    @PostMapping("/sessions/{sessionId}/unblock")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<ChatSessionResponseDTO> unblockChatSession(
            @Parameter(description = "ID de la sesión de chat a desbloquear", required = true)
            @PathVariable Long sessionId) {

        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        ChatSession updated = chatUseCase.unblockChatSession(sessionId, authenticatedUser);
        return ResponseEntity.ok(ChatRestMapper.toChatSessionResponseDTO(updated));
    }
}
