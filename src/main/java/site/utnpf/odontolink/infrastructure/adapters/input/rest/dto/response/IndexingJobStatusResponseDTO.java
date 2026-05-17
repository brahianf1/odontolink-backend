package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Snapshot del estado de un indexing job, expuesto al frontend tras
 * disparar {@code POST /reindex} o {@code POST /documents/{id}/refresh-status}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IndexingJobStatusResponseDTO(
        String jobId,
        String status,
        Instant updatedAt,
        String errorMessage
) {
}
