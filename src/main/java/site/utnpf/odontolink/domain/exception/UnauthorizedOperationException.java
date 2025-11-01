package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando un usuario intenta realizar una operación
 * para la cual no tiene permisos (ej: modificar un recurso que no le pertenece).
 * 
 * Esta excepción es semánticamente diferente de AuthenticationFailedException:
 * - AuthenticationFailedException: El usuario no pudo autenticarse (credenciales inválidas)
 * - UnauthorizedOperationException: El usuario está autenticado pero no tiene permisos para la operación
 */
public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
