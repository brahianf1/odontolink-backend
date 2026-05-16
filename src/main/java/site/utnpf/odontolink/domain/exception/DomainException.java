package site.utnpf.odontolink.domain.exception;

/**
 * Excepción base para todas las excepciones del dominio.
 * Permite un manejo centralizado y tipado de errores de negocio.
 *
 * <p>Opcionalmente lleva un {@code errorCode} estable (p. ej. {@code CHAT_BLOCKED})
 * para que los frontends puedan ramificar UX sin parsear mensajes humanos. El
 * {@code GlobalExceptionHandler} lo propaga al body del 4xx cuando está presente.
 * Los mensajes humanos pueden cambiar; los códigos son contrato.
 */
public abstract class DomainException extends RuntimeException {

    private String errorCode;

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Setter fluido para encadenar el código en sitios que ya construyeron la excepción
     * sin él (útil para no romper constructores existentes).
     */
    public DomainException withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }
}
