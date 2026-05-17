package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sub-objeto {@code spaces_data_source} que va dentro del body del POST a
 * {@code /v2/gen-ai/knowledge_bases/{kb_uuid}/data_sources}.
 *
 * <p>DigitalOcean infiere el endpoint del servicio Spaces desde {@code region};
 * por eso el JSON no incluye un campo {@code endpoint}.
 *
 * <p>{@code item_path} acepta tanto un folder como un archivo especifico. Usamos
 * archivo especifico para mapear 1 documento local ↔ 1 data source remoto.
 */
public record DoSpacesDataSource(
        @JsonProperty("bucket_name") String bucketName,
        @JsonProperty("item_path") String itemPath,
        @JsonProperty("region") String region
) {
}
