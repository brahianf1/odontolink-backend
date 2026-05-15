package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando un usuario intenta realizar una operación
 * para la cual no tiene permisos (ej: modificar un recurso que no le pertenece).
 *
 * Esta excepción es semánticamente diferente de AuthenticationFailedException:
 * - AuthenticationFailedException: El usuario no pudo autenticarse (credenciales inválidas)
 * - UnauthorizedOperationException: El usuario está autenticado pero no tiene permisos para la operación
 *
 * <p>Soporta el {@code errorCode} opcional heredado de {@link DomainException} para
 * distinguir motivos de 403 en el body sin parsear el mensaje humano (p. ej. en chat:
 * {@code CHAT_BLOCKED} vs {@code CHAT_NOT_PARTICIPANT}).
 */
public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedOperationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
