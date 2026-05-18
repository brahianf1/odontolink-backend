package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de {@code POST {AGENT_URL}/api/v1/chat/completions} para DO Gradient.
 *
 * <p>El wire format es deliberadamente igual al de OpenAI chat completions
 * porque DO lo expone bajo el mismo contrato. {@code stream:false} fija el
 * modo sincronico que nuestro adapter usa con RestClient blocking.
 *
 * <p>{@code includeRetrievalInfo:true} pide al proveedor que devuelva el
 * bloque {@code retrieval} con los documentos consultados, lo cual usamos
 * para computar el indicador de confianza (RF34).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoChatCompletionRequest(
        @JsonProperty("messages") List<DoMessage> messages,
        @JsonProperty("stream") boolean stream,
        @JsonProperty("include_retrieval_info") Boolean includeRetrievalInfo
) {
}
