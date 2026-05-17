package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Cuerpo del POST a {@code /v2/gen-ai/indexing_jobs} para disparar la
 * reindexacion de una Knowledge Base.
 *
 * <p>Si {@code dataSourceUuids} viene vacio o null el proveedor indexa
 * todo el contenido pendiente de la KB.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoIndexingJobRequest(
        @JsonProperty("knowledge_base_uuid") String knowledgeBaseUuid,
        @JsonProperty("data_source_uuids") List<String> dataSourceUuids
) {
}
