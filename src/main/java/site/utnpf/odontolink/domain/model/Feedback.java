package site.utnpf.odontolink.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Encuesta multi-criterio sobre una {@link Attention} finalizada.
 *
 * <p>Modela la calificación bidireccional (CU-009/RF21 paciente→practicante,
 * CU-016/RF22 practicante→paciente). Cada feedback agrupa un conjunto de
 * {@link FeedbackCriterionScore} sobre los criterios activos correspondientes
 * a su {@link FeedbackDirection}. El antiguo campo escalar {@code rating}
 * desaparece — la puntuación holística "satisfacción general" pasa a ser
 * un criterio más dentro del set.
 *
 * <p>Reglas de negocio que aplican (validadas en
 * {@code FeedbackPolicyService} y {@code FeedbackCriterionPolicyService}):
 * <ul>
 *   <li>La atención debe estar {@link AttentionStatus#COMPLETED}.</li>
 *   <li>El submittedBy debe ser el paciente o el practicante de la atención.</li>
 *   <li>No más de un feedback por usuario por atención (RF23, también
 *       garantizado por UK a nivel de BD).</li>
 *   <li>Los scores deben cubrir exactamente el set de criterios activos para
 *       la dirección.</li>
 * </ul>
 */
public class Feedback {

    private Long id;
    private Attention attention;
    private User submittedBy;
    private String comment;
    private Instant createdAt;
    private final List<FeedbackCriterionScore> scores = new ArrayList<>();

    public Feedback() {
        this.createdAt = Instant.now();
    }

    public Feedback(Attention attention, User submittedBy, String comment) {
        this();
        this.attention = attention;
        this.submittedBy = submittedBy;
        this.comment = comment;
    }

    /**
     * Agrega un score al feedback estableciendo la relación bidireccional.
     * Idempotente respecto a {@code (feedback, criterion)}: rechaza
     * duplicados para no violar la UK lógica antes de llegar a la BD.
     */
    public void addScore(FeedbackCriterionScore score) {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(score.getCriterion(), "score.criterion");
        boolean duplicate = scores.stream()
                .anyMatch(s -> s.getCriterion() != null
                        && Objects.equals(s.getCriterion().getCode(), score.getCriterion().getCode()));
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Score duplicado para el criterio " + score.getCriterion().getCode());
        }
        score.setFeedback(this);
        scores.add(score);
    }

    public List<FeedbackCriterionScore> getScores() {
        return Collections.unmodifiableList(scores);
    }

    /**
     * Devuelve el score asignado a un criterio por su {@code code}, si existe.
     * Útil para derivar agregados legacy (p.ej. el promedio "global" de
     * satisfacción holística por dirección).
     */
    public Optional<Integer> scoreFor(String criterionCode) {
        if (criterionCode == null) {
            return Optional.empty();
        }
        return scores.stream()
                .filter(s -> s.getCriterion() != null
                        && criterionCode.equalsIgnoreCase(s.getCriterion().getCode()))
                .map(FeedbackCriterionScore::getScore)
                .findFirst();
    }

    public boolean canUserSubmitFeedback(User user) {
        if (attention == null || user == null) {
            return false;
        }
        Patient patient = attention.getPatient();
        if (patient != null && patient.getUser() != null
                && patient.getUser().getId().equals(user.getId())) {
            return true;
        }
        Practitioner practitioner = attention.getPractitioner();
        return practitioner != null && practitioner.getUser() != null
                && practitioner.getUser().getId().equals(user.getId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Attention getAttention() {
        return attention;
    }

    public void setAttention(Attention attention) {
        this.attention = attention;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(User submittedBy) {
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

    /**
     * Reemplaza completamente el conjunto de scores. Usado por mappers de
     * persistencia al rehidratar desde la entidad JPA. Para construcción
     * lógica preferir {@link #addScore(FeedbackCriterionScore)}.
     */
    public void replaceScores(List<FeedbackCriterionScore> incoming) {
        this.scores.clear();
        if (incoming == null) {
            return;
        }
        for (FeedbackCriterionScore s : incoming) {
            if (s != null) {
                s.setFeedback(this);
                scores.add(s);
            }
        }
    }
}
