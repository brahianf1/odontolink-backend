package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de envío de mensaje en el chat.
 * Implementa RF26 - CU 6.2: Enviar un Mensaje.
 *
 * Este DTO solo contiene el contenido del mensaje.
 * El sender se obtiene del usuario autenticado vía JWT (AuthenticationFacade).
 * El chatSessionId se obtiene del path parameter.
 *
 * @author OdontoLink Team
 */
public class SendMessageRequestDTO {

    /**
     * Contenido textual del mensaje.
     * No puede estar vacío y tiene un máximo de 2000 caracteres para evitar abuso del canal
     * (los mensajes operativos del chat clínico no requieren más extensión).
     */
    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
    private String content;

    // Constructores
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
