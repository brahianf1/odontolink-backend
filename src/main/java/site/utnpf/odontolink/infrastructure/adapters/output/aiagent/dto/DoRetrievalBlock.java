package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Bloque {@code retrieval} de la respuesta de chat completions. Esta vacio o
 * ausente cuando el modelo no consulto la KB (respuesta general).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoRetrievalBlock(
        @JsonProperty("retrieved_data") List<DoRetrievalDocument> retrievedData
) {
}
