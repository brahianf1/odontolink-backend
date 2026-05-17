package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort.IndexingJobSnapshot;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.IndexingJobStatusResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.KnowledgeBaseDocumentResponseDTO;

import java.util.List;

/**
 * Mapper estatico entre el dominio de la Knowledge Base y sus DTOs REST.
 */
public final class KnowledgeBaseDocumentRestMapper {

    private KnowledgeBaseDocumentRestMapper() {
    }

    public static KnowledgeBaseDocumentResponseDTO toResponse(KnowledgeBaseDocument domain) {
        if (domain == null) {
            return null;
        }
        KnowledgeBaseDocumentResponseDTO dto = new KnowledgeBaseDocumentResponseDTO();
        dto.setId(domain.getId());
        dto.setTitle(domain.getTitle());
        dto.setKind(domain.getKind());
        dto.setInlineContent(domain.getInlineContent());
        dto.setOriginalFileName(domain.getOriginalFileName());
        dto.setSizeBytes(domain.getSizeBytes());
        dto.setContentType(domain.getContentType());
        dto.setProviderDataSourceId(domain.getProviderDataSourceId());
        dto.setStatus(domain.getStatus());
        dto.setLastIndexingJobId(domain.getLastIndexingJobId());
        dto.setLastIndexedAt(domain.getLastIndexedAt());
        dto.setErrorMessage(domain.getErrorMessage());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }

    public static List<KnowledgeBaseDocumentResponseDTO> toResponseList(List<KnowledgeBaseDocument> domain) {
        return domain.stream()
                .map(KnowledgeBaseDocumentRestMapper::toResponse)
                .toList();
    }

    public static IndexingJobStatusResponseDTO toResponse(IndexingJobSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new IndexingJobStatusResponseDTO(
                snapshot.jobId(),
                snapshot.status(),
                snapshot.updatedAt(),
                snapshot.errorMessage()
        );
    }
}
