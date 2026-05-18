package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;
import java.util.UUID;

/**
 * Sesion de conversacion con el chatbot (RF29).
 *
 * <p>Diseno deliberadamente liviano: solo guarda metadata. El historial real
 * vive en {@link ChatbotMessage} con cap configurable (rolling buffer FIFO).
 *
 * <p>Una sesion pertenece a un usuario autenticado ({@code ownerUserId}) o
 * a un anonimo ({@code anonymousToken}, generado por el backend al primer
 * mensaje sin sesion). Invariante de negocio: exactamente uno de ambos campos
 * no es null. La validacion se aplica en {@link #validateOwnership()}.
 *
 * <p>El {@code anonymousToken} actua como capability: el FE anonimo lo recibe
 * en el primer response y debe reenviarlo en cada mensaje subsiguiente para
 * que el backend pueda recuperar el rolling buffer. Sin ese token, la sesion
 * es inaccesible (no hay forma de adivinarlo).
 *
 * <p>No se migra de anonima a autenticada cuando el usuario hace login a mitad
 * de sesion: seria un vector de hijacking. En su lugar se crea una sesion
 * nueva con {@code ownerUserId}.
 */
public class ChatbotSession {

    private UUID id;
    /** Owner cuando la sesion es de un usuario autenticado. */
    private Long ownerUserId;
    /** Token capability cuando la sesion es anonima. */
    private UUID anonymousToken;
    private Instant startedAt;
    private Instant lastInteractionAt;
    private int messageCount;

    public ChatbotSession() {
    }

    public ChatbotSession(UUID id,
                          Long ownerUserId,
                          UUID anonymousToken,
                          Instant startedAt,
                          Instant lastInteractionAt,
                          int messageCount) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.anonymousToken = anonymousToken;
        this.startedAt = startedAt;
        this.lastInteractionAt = lastInteractionAt;
        this.messageCount = messageCount;
        validateOwnership();
    }

    /**
     * Crea una sesion para un usuario autenticado. El id de la sesion se genera
     * server-side para que el caller no pueda predecirlo.
     */
    public static ChatbotSession forUser(Long userId) {
        if (userId == null) {
            throw new InvalidBusinessRuleException("userId es obligatorio para crear una sesion de usuario.");
        }
        Instant now = Instant.now();
        return new ChatbotSession(UUID.randomUUID(), userId, null, now, now, 0);
    }

    /**
     * Crea una sesion anonima. Genera un {@code anonymousToken} aleatorio que
     * el cliente persiste en su localStorage para reanudar la conversacion.
     */
    public static ChatbotSession forAnonymous() {
        Instant now = Instant.now();
        return new ChatbotSession(UUID.randomUUID(), null, UUID.randomUUID(), now, now, 0);
    }

    /**
     * Marca una nueva interaccion: incrementa el contador y refresca la marca
     * temporal. Se llama una vez por mensaje del usuario procesado con exito
     * (no en errores ni en blocks por PII).
     */
    public void recordInteraction() {
        this.messageCount++;
        this.lastInteractionAt = Instant.now();
    }

    /**
     * Verifica que el caller puede operar sobre esta sesion. Para sesiones de
     * usuario, el {@code authenticatedUserId} debe coincidir. Para sesiones
     * anonimas, el {@code providedAnonymousToken} debe coincidir.
     *
     * <p>Devuelve {@code false} en cualquier mismatch para que el caller pueda
     * responder 404 (no confirma existencia de la sesion).
     */
    public boolean isAccessibleBy(Long authenticatedUserId, UUID providedAnonymousToken) {
        if (this.ownerUserId != null) {
            return authenticatedUserId != null && authenticatedUserId.equals(this.ownerUserId);
        }
        return providedAnonymousToken != null && providedAnonymousToken.equals(this.anonymousToken);
    }

    public boolean isAnonymous() {
        return this.ownerUserId == null;
    }

    private void validateOwnership() {
        boolean hasUser = ownerUserId != null;
        boolean hasAnon = anonymousToken != null;
        if (hasUser == hasAnon) {
            throw new InvalidBusinessRuleException(
                    "La sesion del chatbot debe tener exactamente uno de {ownerUserId, anonymousToken}.");
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getAnonymousToken() {
        return anonymousToken;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public int getMessageCount() {
        return messageCount;
    }
}
