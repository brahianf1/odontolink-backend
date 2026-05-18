package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.AiAgentAccessMode;

/**
 * Snapshot publico de la configuracion del chatbot, pensado para que el FE
 * decida si renderizar el widget de chat y con que mensaje de bienvenida.
 * (RF29).
 *
 * <p>{@code accessGranted=false} significa que el caller no puede iniciar
 * conversacion en este momento (DISABLED, lifecycle != PUBLISHED, o caller sin
 * rol permitido en modo PRIVATE). {@code denyReason} es un codigo estable que
 * el FE puede usar para mostrar mensajes distintos:
 * <ul>
 *   <li>{@code AGENT_DISABLED}: el admin desactivo el chatbot.</li>
 *   <li>{@code AGENT_NOT_PUBLISHED}: el admin todavia no termino la setup
 *       inicial.</li>
 *   <li>{@code AUTHENTICATION_REQUIRED}: modo PRIVATE y caller anonimo.</li>
 *   <li>{@code ROLE_NOT_ALLOWED}: autenticado pero rol no esta en
 *       {@code allowedRoles}.</li>
 * </ul>
 */
public record ChatbotPublicInfo(
        boolean accessGranted,
        AiAgentAccessMode accessMode,
        String displayName,
        String welcomeMessage,
        String denyReason
) {

    public static ChatbotPublicInfo granted(AiAgentAccessMode mode, String displayName, String welcomeMessage) {
        return new ChatbotPublicInfo(true, mode, displayName, welcomeMessage, null);
    }

    public static ChatbotPublicInfo denied(AiAgentAccessMode mode, String reason) {
        return new ChatbotPublicInfo(false, mode, null, null, reason);
    }
}
