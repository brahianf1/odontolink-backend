package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Documento recuperado por RAG dentro del bloque {@code retrieval} de la
 * respuesta de DO Gradient. {@code score} es la similitud coseno [0..1]
 * que usamos para computar el indicador de confianza (RF34).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoRetrievalDocument(
        @JsonProperty("data_source_uuid") String dataSourceUuid,
        @JsonProperty("score") Double score,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("chunk_text") String chunkText
) {
}
