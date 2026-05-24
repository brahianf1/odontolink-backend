package site.utnpf.odontolink.domain.model;

import java.util.Objects;

/**
 * Puntuación 1–5 asignada por un {@link Feedback} a un {@link FeedbackCriterion}.
 *
 * <p>Vive como entidad hija del agregado {@link Feedback}: el ciclo de vida
 * (crear, borrar) se gobierna desde el feedback contenedor. La unicidad
 * lógica {@code (feedback, criterion)} se enforcea a nivel de BD para
 * impedir scoring duplicado por criterio.
 */
public class FeedbackCriterionScore {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;

    private Long id;
    private Feedback feedback;
    private FeedbackCriterion criterion;
    private int score;

    public FeedbackCriterionScore() {
    }

    public FeedbackCriterionScore(FeedbackCriterion criterion, int score) {
        this.criterion = Objects.requireNonNull(criterion, "criterion");
        this.score = validate(score);
    }

    private static int validate(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException(
                    String.format("score debe estar entre %d y %d — recibido: %d",
                            MIN_SCORE, MAX_SCORE, score));
        }
        return score;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public FeedbackCriterion getCriterion() {
        return criterion;
    }

    public void setCriterion(FeedbackCriterion criterion) {
        this.criterion = Objects.requireNonNull(criterion, "criterion");
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = validate(score);
    }
}
