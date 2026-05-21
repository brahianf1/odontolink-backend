package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.AiAgentAccessMode;
import site.utnpf.odontolink.domain.model.AiPiiPolicy;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.Role;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Comando que viaja del controller REST al caso de uso para crear o
 * actualizar el agregado
 * {@link site.utnpf.odontolink.domain.model.AiAgentConfiguration}.
 *
 * <p>Sin defaults: si el admin no provee un campo, la validacion del
 * dominio lo rechaza con 422. Incluye tanto los campos del agente IA
 * (RF31/RF32/RF33) como los del chatbot institucional (RF29/RF34) y el
 * toggle del indicador de confianza categorica.
 */
public record UpdateAiAgentConfigurationCommand(
        String displayName,
        String systemPromptCore,
        String welcomeMessage,
        BigDecimal temperature,
        BigDecimal topP,
        int maxTokens,
        int k,
        AiRetrievalMethod retrievalMethod,
        // Chatbot institucional (RF29/RF31/RF32/RF34)
        AiAgentAccessMode accessMode,
        Set<Role> allowedRoles,
        AiPiiPolicy piiPolicy,
        int conversationBufferSize,
        int rateLimitAnonymousPerHour,
        int rateLimitAuthenticatedPerHour,
        String emergencyBannerText,
        boolean provideCitations,
        boolean showConfidenceIndicator
) {
}
