package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando se viola una regla de negocio específica.
 * 
 * Ejemplos:
 * - Intentar eliminar un tratamiento que tiene turnos activos
 * - Intentar crear una disponibilidad con horarios inválidos
 * - Intentar realizar una operación que viola restricciones del dominio
 * 
 * Esta excepción es más específica que DomainException y proporciona
 * contexto sobre qué regla de negocio fue violada.
 */
public class InvalidBusinessRuleException extends DomainException {

    public InvalidBusinessRuleException(String message) {
        super(message);
    }

    public InvalidBusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
