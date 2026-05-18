package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ChatbotSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para las sesiones del chatbot institucional (RF29).
 *
 * <p>Sesiones son livianas: la conversacion real vive en
 * {@link ChatbotMessageRepository} con cap configurable. Aqui solo se guarda
 * ownership (usuario autenticado o anonymousToken) y contadores.
 */
public interface ChatbotSessionRepository {

    Optional<ChatbotSession> findById(UUID id);

    Optional<ChatbotSession> findByAnonymousToken(UUID anonymousToken);

    ChatbotSession save(ChatbotSession session);

    /** Borra una sesion por id. Idempotente (no falla si no existe). */
    void deleteById(UUID id);

    /** Util para futura limpieza de sesiones idle (hoy no se ejecuta automaticamente). */
    int deleteIdleOlderThan(Instant cutoff);
}
