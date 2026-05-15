package site.utnpf.odontolink.domain.exception;

/**
 * Excepcion lanzada por servicios cuando una operacion excede el limite de
 * intentos permitidos por unidad de tiempo. El {@code GlobalExceptionHandler}
 * la mapea a HTTP 429 (Too Many Requests).
 *
 * <p>El campo {@code retryAfterSeconds} viaja en el header
 * {@code Retry-After} de la respuesta para que el cliente sepa cuanto esperar
 * antes del proximo intento. Se acepta {@code null} si el calculo no aplica.
 */
public class RateLimitExceededException extends RuntimeException {

    private final Long retryAfterSeconds;

    public RateLimitExceededException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
