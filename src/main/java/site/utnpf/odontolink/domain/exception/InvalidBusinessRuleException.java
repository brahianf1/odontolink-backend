package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando se viola una regla de negocio específica.
 *
 * Ejemplos:
 * - Intentar eliminar un tratamiento que tiene turnos activos
 * - Intentar crear una disponibilidad con horarios inválidos
 * - Intentar realizar una operación que viola restricciones del dominio
 *
 * <p>Soporta {@code errorCode} opcional para diferenciar reglas dentro del mismo 422
 * (p. ej. {@code CHAT_ALREADY_BLOCKED}, {@code CHAT_NOT_BLOCKED}). Cuando el
 * {@link GlobalExceptionHandler} lo encuentra, lo propaga al body para que el frontend
 * ramifique UX sin depender del mensaje humano.
 */
public class InvalidBusinessRuleException extends DomainException {

    public InvalidBusinessRuleException(String message) {
        super(message);
    }

    public InvalidBusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
    }
}
