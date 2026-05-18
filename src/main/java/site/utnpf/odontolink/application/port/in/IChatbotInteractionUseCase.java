package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.ChatbotMessageCommand;
import site.utnpf.odontolink.application.port.in.dto.ChatbotPublicInfo;
import site.utnpf.odontolink.domain.model.ChatbotInteractionResult;
import site.utnpf.odontolink.domain.model.Role;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada del chatbot institucional (RF29/RF31/RF32/RF34).
 */
public interface IChatbotInteractionUseCase {

    /**
     * Resuelve el snapshot publico que el FE necesita para decidir si renderizar
     * el chat. Aplica las reglas de {@code accessMode} + {@code allowedRoles}
     * + lifecycle.
     *
     * @param callerRole rol del usuario autenticado, o {@code null} si anonimo.
     */
    ChatbotPublicInfo getPublicInfo(Role callerRole);

    /**
     * Envia un mensaje al chatbot y devuelve la respuesta enriquecida. Crea
     * sesion si no existe, aplica sanitizacion PII, detecta emergencias,
     * llama al proveedor (con circuit breaker), persiste el rolling buffer
     * con cap FIFO y computa el confidence indicator.
     */
    ChatbotInteractionResult sendMessage(ChatbotMessageCommand command);

    /**
     * Cierra una sesion borrando su rolling buffer. Idempotente: devuelve sin
     * error si la sesion ya no existe. Valida ownership (ownerUserId match o
     * anonymousToken match) antes de borrar.
     */
    void closeSession(UUID sessionId, Optional<Long> authenticatedUserId, Optional<UUID> anonymousToken);
}
