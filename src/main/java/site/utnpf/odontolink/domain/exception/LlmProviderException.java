package site.utnpf.odontolink.domain.exception;

/**
 * Excepcion que envuelve fallas del proveedor externo del LLM (RF31-RF33).
 *
 * <p>Se eleva desde los adaptadores de salida ({@code ILlmAgentProviderPort},
 * {@code IKnowledgeBaseProviderPort}) cuando la API del proveedor responde con
 * un error o cuando la comunicacion falla (timeouts, DNS, TLS).
 *
 * <p>El {@link #getStatusCode()} expone el codigo HTTP original si el adapter
 * lo conocia; queda en {@code null} cuando la falla fue antes de obtener
 * respuesta (errores de red).
 *
 * <p>El {@link site.utnpf.odontolink.infrastructure.adapters.input.rest.exception.GlobalExceptionHandler}
 * mapea esta excepcion a {@code 503 Service Unavailable} con
 * {@code errorCode = AI_PROVIDER_UNAVAILABLE} o {@code AI_PROVIDER_BAD_REQUEST}
 * segun haya sido asignado en el constructor, evitando exponer detalles
 * internos del proveedor al cliente.
 */
public class LlmProviderException extends DomainException {

    private final Integer statusCode;

    public LlmProviderException(String message, Integer statusCode, String errorCode) {
        super(message, errorCode);
        this.statusCode = statusCode;
    }

    public LlmProviderException(String message, Integer statusCode, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
        this.statusCode = statusCode;
    }

    public LlmProviderException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
