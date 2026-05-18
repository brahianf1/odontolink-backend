package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Elemento del array {@code choices[]} de la respuesta de chat completions.
 * Para nuestro uso (stream=false) siempre llega exactamente uno con el
 * mensaje del asistente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoChoice(
        @JsonProperty("index") Integer index,
        @JsonProperty("message") DoMessage message,
        @JsonProperty("finish_reason") String finishReason
) {
}
