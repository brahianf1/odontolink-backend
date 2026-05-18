package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Respuesta de {@code POST {AGENT_URL}/api/v1/chat/completions}. Compatible
 * con el wire format de OpenAI mas el bloque {@code retrieval} agregado por
 * DO Gradient para RAG.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoChatCompletionResponse(
        @JsonProperty("id") String id,
        @JsonProperty("model") String model,
        @JsonProperty("choices") List<DoChoice> choices,
        @JsonProperty("usage") DoUsage usage,
        @JsonProperty("retrieval") DoRetrievalBlock retrieval
) {
}
