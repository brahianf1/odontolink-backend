package site.utnpf.odontolink.domain.exception;

/**
 * Excepción base para todas las excepciones del dominio.
 * Permite un manejo centralizado y tipado de errores de negocio.
 */
public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
