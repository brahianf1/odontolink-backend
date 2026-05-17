package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuerpo del POST a {@code /v2/gen-ai/knowledge_bases/{kb_uuid}/data_sources}.
 *
 * <p>La API acepta multiples tipos de data source ({@code spaces_data_source},
 * {@code aws_data_source}, {@code file_upload_data_source}, etc.), pero solo
 * uno de los sub-objetos debe estar presente. Por eso se serializan solo los
 * campos no nulos ({@link JsonInclude.Include#NON_NULL}); en esta version
 * nuestra implementacion solo emite {@code spaces_data_source}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoRegisterDataSourceRequest(
        @JsonProperty("spaces_data_source") DoSpacesDataSource spacesDataSource
) {
}
