package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * DTO de respuesta para una nota de progreso (evolución).
 * Se usa para mostrar el historial de evoluciones de un caso clínico.
 *
 * Contiene la información completa de la nota de progreso.
 *
 * @author OdontoLink Team
 */
public class ProgressNoteResponseDTO {

    private Long id;
    private String note;
    private Instant createdAt;

    // Información del autor
    private Long authorId;
    private String authorName;
    private String authorRole; // PRACTITIONER, SUPERVISOR, etc.

    // ID de la atención (caso) a la que pertenece
    private Long attentionId;

    // Constructores
    public ProgressNoteResponseDTO() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public Long getAttentionId() {
        return attentionId;
    }

    public void setAttentionId(Long attentionId) {
        this.attentionId = attentionId;
    }
}
