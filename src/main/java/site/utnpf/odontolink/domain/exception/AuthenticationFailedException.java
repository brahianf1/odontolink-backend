package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando falla la autenticación.
 */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
