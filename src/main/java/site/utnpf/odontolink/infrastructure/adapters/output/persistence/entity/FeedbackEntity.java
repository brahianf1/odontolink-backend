package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla 'feedbacks'.
 *
 * <p>La columna escalar {@code rating} fue eliminada en la migración a
 * encuesta multi-criterio: las puntuaciones viven ahora en la tabla
 * {@code feedback_criterion_scores} (subcolección {@link #scores}). Con
 * {@code ddl-auto=update} la columna física en MySQL puede quedar huérfana
 * en entornos pre-existentes; en producción se la elimina una sola vez con
 * {@code ALTER TABLE feedbacks DROP COLUMN rating}.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attention_id", nullable = false)
    private AttentionEntity attention;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private UserEntity submittedBy;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "feedback",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<FeedbackCriterionScoreEntity> scores = new ArrayList<>();

    public FeedbackEntity() {
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

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

    public List<FeedbackCriterionScoreEntity> getScores() {
        return scores;
    }

    public void setScores(List<FeedbackCriterionScoreEntity> scores) {
        this.scores = scores;
    }
}
