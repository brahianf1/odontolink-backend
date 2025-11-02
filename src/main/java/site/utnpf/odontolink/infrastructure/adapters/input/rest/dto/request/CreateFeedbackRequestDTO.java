package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.*;

/**
 * DTO para la solicitud de creación de feedback sobre una atención.
 * Implementa CU-009, CU-016 (RF21, RF22): Calificar Paciente/Practicante.
 *
 * Este DTO captura la intención del usuario (paciente o practicante) de enviar
 * su calificación sobre una atención finalizada.
 *
 * @author OdontoLink Team
 */
public class CreateFeedbackRequestDTO {

    /**
     * ID de la atención sobre la que se envía el feedback.
     * Debe ser una atención que esté en estado COMPLETED.
     */
    @NotNull(message = "El ID de la atención es obligatorio")
    @Positive(message = "El ID de la atención debe ser positivo")
    private Long attentionId;

    /**
     * Calificación en escala de 1 a 5 estrellas.
     */
    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1 estrella")
    @Max(value = 5, message = "La calificación máxima es 5 estrellas")
    private Integer rating;

    /**
     * Comentario opcional del usuario.
     * Si se proporciona, debe tener un contenido significativo.
     */
    @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres")
    private String comment;

    // Constructores
    public CreateFeedbackRequestDTO() {
    }

    public CreateFeedbackRequestDTO(Long attentionId, Integer rating, String comment) {
        this.attentionId = attentionId;
        this.rating = rating;
        this.comment = comment;
    }

    // Getters y Setters
    public Long getAttentionId() {
        return attentionId;
    }

    public void setAttentionId(Long attentionId) {
        this.attentionId = attentionId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
