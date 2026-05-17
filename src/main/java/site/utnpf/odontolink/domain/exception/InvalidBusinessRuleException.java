package site.utnpf.odontolink.domain.exception;

import java.util.Collections;
import java.util.List;

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
 *
 * <p>Tambien permite acompañar el error con una lista de {@code details} estructurada
 * (codigos estables, no humanos): util cuando la regla de negocio reporta varias
 * causas simultaneas (p. ej. {@code REQUIRES_SYSTEM_PROMPT, REQUIRES_GUARDRAILS}
 * al fallar el publish del agente IA). El handler los expone en el campo
 * {@code details[]} del payload para que el frontend los pinte uno por uno sin
 * parsear el mensaje humano.
 */
public class InvalidBusinessRuleException extends DomainException {

    private final List<String> details;

    public InvalidBusinessRuleException(String message) {
        super(message);
        this.details = Collections.emptyList();
    }

    public InvalidBusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.details = Collections.emptyList();
    }

    public InvalidBusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
        this.details = Collections.emptyList();
    }

    public InvalidBusinessRuleException(String message, String errorCode, List<String> details) {
        super(message, errorCode);
        this.details = details == null ? Collections.emptyList() : List.copyOf(details);
    }

    /**
     * Lista de codigos estables que detallan el error (p. ej. los requisitos
     * faltantes en un publish). Nunca {@code null}; vacia si la excepcion no
     * los aporta.
     */
    public List<String> getDetails() {
        return details;
    }
}
