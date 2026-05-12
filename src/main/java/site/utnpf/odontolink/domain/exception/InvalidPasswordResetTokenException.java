package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando el token de recuperación de contraseña
 * presentado por el usuario no es válido: inexistente, expirado o ya consumido.
 *
 * Se modela como excepción específica para que el manejador global pueda
 * traducirla a un HTTP 400 con mensaje coherente, sin filtrar detalles que
 * permitan inferir el motivo exacto del rechazo (estrategia defensiva).
 */
public class InvalidPasswordResetTokenException extends DomainException {

    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}
