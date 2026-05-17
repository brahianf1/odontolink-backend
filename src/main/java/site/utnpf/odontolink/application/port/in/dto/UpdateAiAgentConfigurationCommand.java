package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.AiRetrievalMethod;

import java.math.BigDecimal;

/**
 * Comando que viaja del controller REST al caso de uso para crear o
 * actualizar el agregado
 * {@link site.utnpf.odontolink.domain.model.AiAgentConfiguration}.
 *
 * <p>Sin defaults: si el admin no provee un campo, la validacion del
 * dominio lo rechaza con 422.
 */
public record UpdateAiAgentConfigurationCommand(
        String displayName,
        String systemPromptCore,
        String welcomeMessage,
        BigDecimal temperature,
        BigDecimal topP,
        int maxTokens,
        int k,
        AiRetrievalMethod retrievalMethod
) {
}
