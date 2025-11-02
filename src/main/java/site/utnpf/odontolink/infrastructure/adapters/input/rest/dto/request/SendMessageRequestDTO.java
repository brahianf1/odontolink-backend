package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitud de envío de mensaje en el chat.
 * Implementa CU 6.2: Enviar un Mensaje (RF26).
 *
 * Este DTO solo contiene el contenido del mensaje.
 * El sender se obtiene del usuario autenticado (@AuthenticationPrincipal).
 * El chatSessionId se obtiene del path parameter.
 *
 * @author OdontoLink Team
 */
public class SendMessageRequestDTO {

    /**
     * Contenido textual del mensaje.
     * No puede estar vacío y tiene un máximo de 2000 caracteres.
     */
    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
    private String content;

    // Constructor sin argumentos (requerido por Jackson)
    public SendMessageRequestDTO() {
    }

    public SendMessageRequestDTO(String content) {
        this.content = content;
    }

    // Getters y Setters

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
