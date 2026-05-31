package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO de respuesta para una nota de progreso (evolución).
 * Se usa para mostrar el historial de evoluciones de un caso clínico.
 *
 * Contiene la información completa de la nota de progreso.
 *
 * @author OdontoLink Team
 */
@Schema(description = "Nota de evolución registrada en el expediente clínico. Inmutable una vez creada; " +
        "incluye autor, rol y timestamp del servidor.")
public class ProgressNoteResponseDTO {

    @Schema(description = "Identificador único de la nota de evolución.", example = "101")
    private Long id;

    @Schema(description = "Texto de la evolución clínica.",
            example = "Se realiza profilaxis completa. Paciente tolera bien el procedimiento. Se indica " +
                    "control en 6 meses y refuerzo de técnica de cepillado.")
    private String note;

    @Schema(description = "Marca de tiempo (UTC, ISO-8601 con zona) en la que se registró la evolución.",
            example = "2025-11-15T13:45:00Z")
    private Instant createdAt;

    // Información del autor
    @Schema(description = "ID del usuario autor de la nota.", example = "8")
    private Long authorId;

    @Schema(description = "Nombre completo del autor de la nota.", example = "Ana Martinez")
    private String authorName;

    @Schema(description = "URL pública de la foto de perfil del autor de la nota. Null si no tiene foto.",
            example = "https://cdn.odontolink/u/8/avatar.jpg", nullable = true)
    private String authorProfilePictureUrl;

    @Schema(description = "Rol del autor de la nota (mayormente `ROLE_PRACTITIONER`; los supervisores " +
            "pueden aparecer si registraron observaciones desde su propio módulo).",
            example = "ROLE_PRACTITIONER",
            allowableValues = {"ROLE_PRACTITIONER", "ROLE_SUPERVISOR", "ROLE_ADMIN"})
    private String authorRole;

    // ID de la atención (caso) a la que pertenece
    @Schema(description = "ID del caso clínico (Attention) al que pertenece la nota.", example = "23")
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

    public String getAuthorProfilePictureUrl() {
        return authorProfilePictureUrl;
    }

    public void setAuthorProfilePictureUrl(String authorProfilePictureUrl) {
        this.authorProfilePictureUrl = authorProfilePictureUrl;
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
