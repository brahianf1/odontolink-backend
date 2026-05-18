package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IChatbotInteractionUseCase;
import site.utnpf.odontolink.application.port.in.dto.ChatbotMessageCommand;
import site.utnpf.odontolink.application.port.in.dto.ChatbotPublicInfo;
import site.utnpf.odontolink.domain.model.ChatbotInteractionResult;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ChatbotMessageRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatbotMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatbotPublicInfoResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.ChatbotRestMapper;

import java.util.Optional;
import java.util.UUID;

/**
 * Controller publico del chatbot institucional (RF29/RF31/RF32/RF34).
 *
 * <p>Sin {@code @PreAuthorize}: el acceso lo controla el use case en runtime
 * leyendo {@code AiAgentConfiguration.accessMode} + {@code allowedRoles}. Esto
 * permite que un cambio del admin en el panel apague el endpoint sin redeploy.
 * El filter JWT que corre antes deja el request pasar tanto autenticado como
 * anonimo (no exige token); aqui resolvemos el principal si esta presente.
 */
@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot Publico",
        description = "Endpoint conversacional del chatbot institucional (RF29/RF31/RF32/RF34)")
public class PublicChatbotController {

    private final IChatbotInteractionUseCase chatbotUseCase;
    private final UserRepository userRepository;

    public PublicChatbotController(IChatbotInteractionUseCase chatbotUseCase,
                                   UserRepository userRepository) {
        this.chatbotUseCase = chatbotUseCase;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Informacion publica del chatbot",
            description = "Devuelve si el caller puede usar el chatbot (segun accessMode + role) y el " +
                    "welcomeMessage / displayName configurados. Sin auth: caller anonimo. Con auth: el " +
                    "FE recibe accessGranted=true solo si el rol esta permitido cuando el modo es PRIVATE.")
    @GetMapping("/info")
    public ResponseEntity<ChatbotPublicInfoResponseDTO> getInfo() {
        Role callerRole = resolveCallerRole().orElse(null);
        ChatbotPublicInfo info = chatbotUseCase.getPublicInfo(callerRole);
        return ResponseEntity.ok(ChatbotRestMapper.toResponse(info));
    }

    @Operation(summary = "Enviar mensaje al chatbot",
            description = "Procesa el mensaje con sanitizacion PII + deteccion de emergencias + RAG. " +
                    "Devuelve la respuesta + confidence (RF34) + flags estructuradas. Crea sesion si " +
                    "no se envia sessionId. Para sesiones anonimas el FE debe persistir y reenviar el " +
                    "anonymousToken.")
    @PostMapping("/messages")
    public ResponseEntity<ChatbotMessageResponseDTO> sendMessage(
            @Valid @RequestBody ChatbotMessageRequestDTO request,
            HttpServletRequest httpRequest) {
        Optional<Long> userId = resolveAuthenticatedUserId();
        ChatbotMessageCommand cmd = new ChatbotMessageCommand(
                request.getMessage(),
                Optional.ofNullable(request.getSessionId()),
                Optional.ofNullable(request.getAnonymousToken()),
                userId,
                resolveClientIp(httpRequest)
        );
        ChatbotInteractionResult result = chatbotUseCase.sendMessage(cmd);
        return ResponseEntity.ok(ChatbotRestMapper.toResponse(result));
    }

    @Operation(summary = "Cerrar sesion del chatbot",
            description = "Borra el rolling buffer y la sesion. Idempotente: 204 incluso si la sesion " +
                    "ya no existia. Valida ownership: anonimo debe pasar anonymousToken; autenticado " +
                    "debe ser el dueno.")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> closeSession(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) UUID anonymousToken) {
        Optional<Long> userId = resolveAuthenticatedUserId();
        chatbotUseCase.closeSession(sessionId, userId, Optional.ofNullable(anonymousToken));
        return ResponseEntity.noContent().build();
    }

    /**
     * Resuelve el rol del caller via SecurityContext. Devuelve vacio para
     * anonimos. Tomamos solo la primera autoridad que mapee a un Role
     * conocido. Las autoridades de Spring Security llegan con prefijo
     * {@code ROLE_PATIENT} etc., que coincide 1:1 con nuestro enum.
     */
    private Optional<Role> resolveCallerRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            return Optional.empty();
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            try {
                return Optional.of(Role.valueOf(ga.getAuthority()));
            } catch (IllegalArgumentException ignored) {
                // Autoridad no es un Role del dominio (scopes, etc.). Probamos siguiente.
            }
        }
        return Optional.empty();
    }

    private Optional<Long> resolveAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .map(User::getId);
        }
        return Optional.empty();
    }

    private static boolean isAnonymous(Authentication auth) {
        return "anonymousUser".equals(String.valueOf(auth.getPrincipal()));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
