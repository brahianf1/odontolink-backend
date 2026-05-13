package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de bloqueo de una sesión de chat.
 * Implementa RF28 - Historia #17: Bloqueo del paciente por parte del practicante.
 *
 * El body es totalmente opcional: si se omite, el bloqueo se registra sin motivo.
 * Si se incluye, el motivo se persiste como parte del audit trail (blockedAt, blockedByUser, etc.).
 *
 * @author OdontoLink Team
 */
public class BlockChatSessionRequestDTO {

    /**
     * Motivo del bloqueo. Opcional.
     * Forma parte del audit trail: queda disponible para que un futuro supervisor pueda revisar
     * la decisión clínica del practicante. Limitado a 500 caracteres para que quepa en una
     * columna VARCHAR sin necesidad de TEXT.
     */
    @Size(max = 500, message = "El motivo del bloqueo no puede exceder 500 caracteres")
    private String reason;

    // Constructores
    public BlockChatSessionRequestDTO() {
    }

    public BlockChatSessionRequestDTO(String reason) {
        this.reason = reason;
    }

    // Getters y Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
