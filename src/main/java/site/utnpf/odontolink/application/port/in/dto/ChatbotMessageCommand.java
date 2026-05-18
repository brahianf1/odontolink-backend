package site.utnpf.odontolink.application.port.in.dto;

import java.util.Optional;
import java.util.UUID;

/**
 * Comando que viaja del controller REST al use case de envio de mensajes al
 * chatbot (RF29/RF31/RF32/RF34).
 *
 * <p>Mezcla datos del cuerpo del request ({@code message}, {@code sessionId},
 * {@code anonymousToken}) con datos del contexto ({@code authenticatedUserId},
 * {@code clientIp}) que el controller resuelve antes de invocar al use case.
 * Asi el use case queda totalmente desacoplado de HttpServletRequest.
 */
public record ChatbotMessageCommand(
        String message,
        Optional<UUID> sessionId,
        Optional<UUID> anonymousToken,
        Optional<Long> authenticatedUserId,
        String clientIp
) {
}
