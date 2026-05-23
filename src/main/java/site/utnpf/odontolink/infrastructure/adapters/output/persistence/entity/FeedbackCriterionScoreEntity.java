package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA del score asignado por un {@link FeedbackEntity} a un
 * {@link FeedbackCriterionEntity}. UK lógica
 * {@code (feedback_id, criterion_id)} impide scoring duplicado.
 */
@Entity
@Table(name = "feedback_criterion_scores",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_fcs_feedback_criterion",
                    columnNames = {"feedback_id", "criterion_id"})
        },
        indexes = {
            @Index(name = "idx_fcs_criterion", columnList = "criterion_id"),
            @Index(name = "idx_fcs_feedback", columnList = "feedback_id")
        })
public class FeedbackCriterionScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private FeedbackEntity feedback;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private FeedbackCriterionEntity criterion;

    @Column(name = "score", nullable = false)
    private int score;

    public FeedbackCriterionScoreEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeedbackEntity getFeedback() {
        return feedback;
    }

    public void setFeedback(FeedbackEntity feedback) {
        this.feedback = feedback;
    }

    public FeedbackCriterionEntity getCriterion() {
        return criterion;
    }

    public void setCriterion(FeedbackCriterionEntity criterion) {
        this.criterion = criterion;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
