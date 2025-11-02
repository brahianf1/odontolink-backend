package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entidad JPA para la tabla 'feedbacks'.
 * Representa el feedback (calificación) sobre una atención en la base de datos.
 *
 * Esta entidad modela tanto "Calificar Paciente" (CU-009) como "Calificar Practicante" (CU-016).
 * Un feedback siempre está asociado a una Attention y a un User (submittedBy).
 *
 * @author OdontoLink Team
 */
@Entity
@Table(name = "feedbacks",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_feedback_attention_user",
                columnNames = {"attention_id", "submitted_by_id"}
            )
        },
        indexes = {
            @Index(name = "idx_feedback_attention", columnList = "attention_id"),
            @Index(name = "idx_feedback_submitted_by", columnList = "submitted_by_id")
        })
public class FeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación ManyToOne con AttentionEntity.
     * Un feedback está siempre asociado a una atención específica.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attention_id", nullable = false)
    private AttentionEntity attention;

    /**
     * Relación ManyToOne con UserEntity.
     * Representa el usuario que envió el feedback (paciente o practicante).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private UserEntity submittedBy;

    /**
     * Calificación en escala de 1 a 5 estrellas.
     */
    @Column(nullable = false)
    private int rating;

    /**
     * Comentario opcional del usuario.
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Timestamp de creación del feedback.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Constructor sin argumentos (requerido por JPA)
    public FeedbackEntity() {
        this.createdAt = Instant.now();
    }

    // Callbacks de JPA
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttentionEntity getAttention() {
        return attention;
    }

    public void setAttention(AttentionEntity attention) {
        this.attention = attention;
    }

    public UserEntity getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UserEntity submittedBy) {
        this.submittedBy = submittedBy;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
