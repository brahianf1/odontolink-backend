package site.utnpf.odontolink.domain.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resultado completo de un turno de conversacion con el chatbot (RF29/RF31/RF32/RF34).
 *
 * <p>Estructura "rica" pensada para que el FE renderice variaciones de UI sin
 * parsear strings:
 * <ul>
 *   <li>{@code confidence} (RF34): 0-100. {@code null} cuando aplica fallback o
 *       cuando hay {@code emergencyDetected} (en emergencias el foco es la
 *       derivacion, no la confianza).</li>
 *   <li>{@code basedOnKnowledgeBase}: {@code true} si el proveedor uso RAG (hubo
 *       documentos recuperados con score). {@code false} si fue una respuesta
 *       general del modelo. El FE puede mostrar un badge "respuesta general"
 *       para que el paciente sepa si la fuente viene del corpus curado.</li>
 *   <li>{@code emergencyDetected}: el detector local (no el LLM) marco una o mas
 *       keywords criticas. El {@code reply} ya viene con el banner antepuesto.</li>
 *   <li>{@code piiBlocked}: la politica era BLOCK y se detecto PII. El bot
 *       respondio con un texto educativo y NO se llamo al proveedor.</li>
 *   <li>{@code detectedPiiTypes}: lista de tipos detectados (vacia si no hubo
 *       deteccion). Util para que el FE muestre que tipo de dato se detecto.</li>
 *   <li>{@code fallbackTriggered}: la llamada al proveedor fallo (circuit abierto
 *       o excepcion irrecuperable) y se respondio con un mensaje fijo amigable.
 *       {@code latencyMs} sigue reflejando el tiempo invertido.</li>
 * </ul>
 *
 * <p>Tambien se devuelve siempre el {@code anonymousToken} cuando la sesion es
 * anonima: en el primer turno el FE no lo conoce y necesita guardarlo para
 * reanudar la conversacion. En turnos subsiguientes vuelve a viajar para que el
 * FE pueda verificar consistencia.
 */
public record ChatbotInteractionResult(
        UUID sessionId,
        UUID anonymousToken,
        String reply,
        Integer confidence,
        boolean basedOnKnowledgeBase,
        boolean emergencyDetected,
        boolean piiBlocked,
        Set<ChatbotPiiType> detectedPiiTypes,
        boolean fallbackTriggered,
        long latencyMs,
        List<String> retrievedDocumentIds
) {

    public static ChatbotInteractionResult piiBlocked(UUID sessionId,
                                                     UUID anonymousToken,
                                                     String educationalReply,
                                                     Set<ChatbotPiiType> detected,
                                                     long latencyMs) {
        return new ChatbotInteractionResult(
                sessionId, anonymousToken, educationalReply,
                null, false, false, true, detected, false, latencyMs, List.of());
    }

    public static ChatbotInteractionResult fallback(UUID sessionId,
                                                   UUID anonymousToken,
                                                   String fallbackReply,
                                                   long latencyMs) {
        return new ChatbotInteractionResult(
                sessionId, anonymousToken, fallbackReply,
                null, false, false, false, Set.of(), true, latencyMs, List.of());
    }
}
