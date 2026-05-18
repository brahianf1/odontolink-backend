package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Payload de {@code POST /api/chatbot/messages} (RF29).
 *
 * <p>{@code message} es el texto del usuario. {@code sessionId} y
 * {@code anonymousToken} son opcionales: el primer turno se manda sin ellos
 * y el backend genera ambos en la respuesta. Los siguientes deben reenviar
 * los dos (capability del anonimo / continuidad del autenticado).
 */
public class ChatbotMessageRequestDTO {

    @NotBlank
    @Size(min = 1, max = 2000)
    private String message;

    private UUID sessionId;

    private UUID anonymousToken;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getAnonymousToken() { return anonymousToken; }
    public void setAnonymousToken(UUID anonymousToken) { this.anonymousToken = anonymousToken; }
}
