package site.utnpf.odontolink.infrastructure.config.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import site.utnpf.odontolink.application.port.out.IChatbotRateLimitPolicyPort;
import site.utnpf.odontolink.application.port.out.IChatbotRateLimitPolicyPort.ChatbotRateLimits;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ErrorResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;

import java.io.IOException;

/**
 * Filter de rate limiting especifico del chatbot institucional (RF29).
 *
 * <p>A diferencia del {@link RateLimitingFilter} general (que corre ANTES del
 * JWT para parar abusos anonimos al login), este corre DESPUES del JWT para
 * poder discriminar autenticado vs anonimo y aplicar cap distinto a cada uno.
 * El admin define ambos caps desde {@code AiAgentConfiguration}.
 *
 * <p>El filter no consulta directo a BD: usa {@link IChatbotRateLimitPolicyPort}
 * que tiene caching local con TTL para no martillar la BD en cada request.
 *
 * <p>Solo se aplica a {@code POST /api/chatbot/messages}: los demas endpoints
 * del chatbot (info, close-session) son livianos y se rate-limitean solo
 * via el filter general.
 */
@Component
public class ChatbotRateLimitingFilter extends OncePerRequestFilter {

    private static final String CHATBOT_MESSAGES_PATH = "/api/chatbot/messages";

    private final RateLimitRegistry registry;
    private final IChatbotRateLimitPolicyPort policyPort;
    private final ObjectMapper objectMapper;

    public ChatbotRateLimitingFilter(RateLimitRegistry registry,
                                     IChatbotRateLimitPolicyPort policyPort,
                                     ObjectMapper objectMapper) {
        this.registry = registry;
        this.policyPort = policyPort;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !CHATBOT_MESSAGES_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        ChatbotRateLimits limits = policyPort.getCurrentLimits();
        String policy;
        String key;
        long capacity;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(auth)) {
            policy = RateLimitRegistry.CHATBOT_AUTHENTICATED_USER;
            key = "user:" + extractPrincipalName(auth);
            capacity = limits.authenticatedPerHour();
        } else {
            policy = RateLimitRegistry.CHATBOT_ANONYMOUS_IP;
            key = "ip:" + resolveClientIp(request);
            capacity = limits.anonymousPerHour();
        }

        Bucket bucket = registry.resolveDynamic(policy, key, capacity);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfter));
        ErrorResponseDTO errorBody = new ErrorResponseDTO(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Demasiados mensajes en poco tiempo. Esperá unos minutos antes de volver a intentarlo.",
                request.getRequestURI()
        );
        errorBody.setErrorCode(AiAgentErrorCodes.AI_RATE_LIMIT_EXCEEDED);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        response.getWriter().flush();
    }

    private static boolean isAuthenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));
    }

    private static String extractPrincipalName(Authentication auth) {
        if (auth.getPrincipal() instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return String.valueOf(auth.getPrincipal());
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
