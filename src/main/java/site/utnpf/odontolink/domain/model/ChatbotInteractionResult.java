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
 *   <li>{@code assessment} (RF34): categoria visible al paciente + score
 *       numerico interno. {@code null} cuando aplica fallback, emergencia,
 *       PII bloqueado o el admin desactivo el indicador
 *       ({@code showConfidenceIndicator=false} en {@code AiAgentConfiguration}).</li>
 *   <li>{@code emergencyDetected}: el detector local (no el LLM) marco una o mas
 *       keywords criticas. El {@code reply} ya viene con el banner antepuesto.</li>
 *   <li>{@code piiBlocked}: la politica era BLOCK y se detecto PII. El bot
 *       respondio con un texto educativo y NO se llamo al proveedor.</li>
 *   <li>{@code detectedPiiTypes}: lista de tipos detectados (vacia si no hubo
 *       deteccion). Util para que el FE muestre que tipo de dato se detecto.</li>
 *   <li>{@code fallbackTriggered}: la llamada al proveedor fallo (circuit abierto
 *       o excepcion irrecuperable) y se respondio con un mensaje fijo amigable.
 *       {@code latencyMs} sigue reflejando el tiempo invertido.</li>
 *   <li>{@code retrievedDocumentIds}: identificadores de los data sources que
 *       activaron el RAG en este turno. Util para admin/auditoria/logs. La capa
 *       REST publica NO lo expone al paciente (decision RF34: el usuario no
 *       referencia documentos, ve solo la categoria de confianza).</li>
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
        ConfidenceAssessment assessment,
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
                null, false, true, detected, false, latencyMs, List.of());
    }

    public static ChatbotInteractionResult fallback(UUID sessionId,
                                                   UUID anonymousToken,
                                                   String fallbackReply,
                                                   long latencyMs) {
        return new ChatbotInteractionResult(
                sessionId, anonymousToken, fallbackReply,
                null, false, false, Set.of(), true, latencyMs, List.of());
    }
}
