package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de {@code POST {AGENT_URL}/api/v1/chat/completions?agent=true} para
 * DO Gradient.
 *
 * <p>El wire format es deliberadamente igual al de OpenAI chat completions
 * porque DO lo expone bajo el mismo contrato. {@code stream:false} fija el
 * modo sincronico que nuestro adapter usa con RestClient blocking.
 *
 * <p>{@code model} es opcional: cuando se llama al endpoint del agente con
 * {@code ?agent=true} el modelo lo provee el agente deployado en DO. Lo
 * exponemos como opcional por si el operador necesita forzarlo (override) en
 * algun entorno; en operacion normal queda null y se omite del JSON.
 *
 * <p>{@code include_retrieval_info} es <strong>obligatorio</strong> para que
 * DO Gradient devuelva el bloque {@code retrieval} en la respuesta (chunks
 * RAG + scores). Sin este flag, la respuesta llega sin {@code retrieval} y el
 * indicador de confianza (RF34) pierde su senal principal — el bug del "50%
 * siempre" descubierto en el PoC de mayo 2026. El valor por defecto del
 * record es {@code true} para que nadie pueda olvidar setearlo. Documentacion
 * oficial: docs.digitalocean.com/products/gradient-ai-platform/how-to/use-agents/.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoChatCompletionRequest(
        @JsonProperty("messages") List<DoMessage> messages,
        @JsonProperty("stream") boolean stream,
        @JsonProperty("model") String model,
        @JsonProperty("include_retrieval_info") boolean includeRetrievalInfo
) {

    /**
     * Constructor de conveniencia que fija {@code includeRetrievalInfo=true}.
     * Es el patron correcto para invocaciones del chatbot que necesitan el
     * bloque {@code retrieval} para computar la confianza (RF34).
     */
    public static DoChatCompletionRequest withRetrievalInfo(List<DoMessage> messages, String model) {
        return new DoChatCompletionRequest(messages, false, model, true);
    }
}
