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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ErrorResponseDTO;

import java.io.IOException;

/**
 * Filtro de rate limiting basado en IP del cliente para los endpoints
 * publicos sensibles a abuso (auth y password reset).
 *
 * <p>Politicas aplicadas:
 * <ul>
 *   <li>{@code POST /api/auth/forgot-password}: 5/IP/h. Al exceder, devuelve
 *       <strong>202 silenciado</strong> con el mismo body que en el caso
 *       normal — anti-enumeracion: el cliente no puede distinguir si fue
 *       rechazado por rate-limit o si simplemente el email no existia.</li>
 *   <li>{@code POST /api/auth/reset-password}: 10/IP/h. Al exceder, 429
 *       con header {@code Retry-After}.</li>
 *   <li>{@code POST /api/auth/login}: 10/IP/min. Al exceder, 429.</li>
 * </ul>
 *
 * <p>Otros rate limits con clave distinta a IP (email, userId) se aplican
 * dentro de los servicios respectivos para tener acceso al payload o al
 * contexto autenticado.
 *
 * <p>Resolucion de IP: prioriza {@code X-Forwarded-For} (entornos con proxy
 * reverso como Traefik en Dokploy); fallback a {@code RemoteAddr} si no
 * esta presente.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String FORGOT_PATH = "/api/auth/forgot-password";
    private static final String RESET_PATH = "/api/auth/reset-password";
    private static final String LOGIN_PATH = "/api/auth/login";

    private static final String SILENCED_BODY =
            "{\"message\":\"Si el email se encuentra registrado, se enviarán las instrucciones para restablecer la contraseña.\"}";

    private final RateLimitRegistry registry;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String policy = resolvePolicy(path);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = registry.resolve(policy, clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfterSeconds));

        if (FORGOT_PATH.equals(path)) {
            // Anti-enumeracion: respondemos como si todo hubiese salido bien.
            writeJson(response, HttpStatus.ACCEPTED, SILENCED_BODY);
        } else {
            // Mismo shape que GlobalExceptionHandler para que el FE pueda
            // parsear errores con un unico parser uniforme. El filter no pasa
            // por @ControllerAdvice, asi que construimos ErrorResponseDTO a mano.
            ErrorResponseDTO errorBody = new ErrorResponseDTO(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Demasiados intentos. Vuelva a intentar más tarde.",
                    path
            );
            writeJson(response, HttpStatus.TOO_MANY_REQUESTS, objectMapper.writeValueAsString(errorBody));
        }
    }

    private String resolvePolicy(String path) {
        if (FORGOT_PATH.equals(path)) {
            return RateLimitRegistry.FORGOT_PASSWORD_IP;
        }
        if (RESET_PATH.equals(path)) {
            return RateLimitRegistry.RESET_PASSWORD_IP;
        }
        if (LOGIN_PATH.equals(path)) {
            return RateLimitRegistry.LOGIN_IP;
        }
        return null;
    }

    /**
     * Devuelve la IP del cliente respetando proxies reversos. El header
     * {@code X-Forwarded-For} puede contener varias IPs separadas por coma
     * cuando hay multiples saltos; la convencion es que la primera es la del
     * cliente original.
     */
    private String resolveClientIp(HttpServletRequest request) {
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

    private void writeJson(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
        response.getWriter().flush();
    }
}
