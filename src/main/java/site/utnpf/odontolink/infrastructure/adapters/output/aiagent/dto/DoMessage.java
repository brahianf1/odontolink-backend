package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mensaje individual del wire format de chat completions (compatible con
 * OpenAI), usado por DigitalOcean Gradient.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content
) {
}
