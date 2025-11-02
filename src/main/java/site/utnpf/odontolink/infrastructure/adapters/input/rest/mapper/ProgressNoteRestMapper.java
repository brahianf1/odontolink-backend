package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ProgressNoteResponseDTO;

/**
 * Mapper para convertir objetos de dominio ProgressNote a DTOs de respuesta.
 *
 * Responsabilidad:
 * - Conversión Dominio → DTO: Convierte ProgressNotes del dominio a DTOs para respuestas HTTP
 *
 * Este mapper construye un DTO completo que incluye información del autor de la nota.
 *
 * Implementado para responder al RF11 - CU 4.2: "Registrar Evolución".
 *
 * @author OdontoLink Team
 */
public class ProgressNoteRestMapper {

    private ProgressNoteRestMapper() {
        // Utility class
    }

    /**
     * Convierte una ProgressNote del dominio a DTO de respuesta.
     *
     * Extrae información de:
     * - ProgressNote: id, note, createdAt
     * - Author (User): id, nombre completo, rol
     * - Attention: id del caso clínico
     *
     * @param domain Objeto de dominio ProgressNote (nota de progreso)
     * @return DTO para respuesta HTTP con toda la información necesaria
     */
    public static ProgressNoteResponseDTO toResponse(ProgressNote domain) {
        if (domain == null) {
            return null;
        }

        ProgressNoteResponseDTO response = new ProgressNoteResponseDTO();
        response.setId(domain.getId());
        response.setNote(domain.getNote());
        response.setCreatedAt(domain.getCreatedAt());

        // Información del autor
        if (domain.getAuthor() != null) {
            response.setAuthorId(domain.getAuthor().getId());
            response.setAuthorName(
                    domain.getAuthor().getFirstName() + " " +
                            domain.getAuthor().getLastName()
            );
            if (domain.getAuthor().getRole() != null) {
                response.setAuthorRole(domain.getAuthor().getRole().toString());
            }
        }

        // Información de la atención (caso)
        if (domain.getAttention() != null) {
            response.setAttentionId(domain.getAttention().getId());
        }

        return response;
    }
}
