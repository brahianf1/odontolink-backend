package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Representa el Feedback (calificación) sobre una Atención.
 * Modela tanto "Calificar Paciente" (CU-009, RF21) como "Calificar Practicante" (CU-016, RF22).
 *
 * Un Feedback está siempre vinculado a:
 * - Una Attention (caso clínico finalizado)
 * - Un User que lo envía (submittedBy)
 *
 * Reglas de negocio implementadas:
 * - El rating debe estar entre 1 y 5 estrellas
 * - El feedback solo puede crearse cuando la Attention está COMPLETED
 * - El submittedBy debe ser el Paciente o el Practicante de la Attention
 *
 * @author OdontoLink Team
 */
public class Feedback {
    private Long id;

    /** Relación N-a-1: El feedback es SIEMPRE sobre un caso/atención */
    private Attention attention;

    /** Relación N-a-1: El User que escribió este feedback */
    private User submittedBy;

    /** Calificación en escala de 1 a 5 estrellas */
    private int rating;

    /** Comentario opcional del usuario */
    private String comment;

    /** Timestamp de creación del feedback */
    private Instant createdAt;

    // Constantes para validación
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    // Constructor sin argumentos (requerido por mappers de persistencia)
    public Feedback() {
        this.createdAt = Instant.now();
    }

    /**
     * Constructor con validaciones de negocio.
     *
     * @param attention La atención asociada (debe estar COMPLETED)
     * @param submittedBy El usuario que envía el feedback
     * @param rating Calificación (1-5)
     * @param comment Comentario opcional
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    public Feedback(Attention attention, User submittedBy, int rating, String comment) {
        this();
        validateRating(rating);
        this.attention = attention;
        this.submittedBy = submittedBy;
        this.rating = rating;
        this.comment = comment;
    }

    /**
     * Valida que el rating esté en el rango permitido (1-5).
     *
     * @param rating La calificación a validar
     * @throws IllegalArgumentException si el rating está fuera del rango
     */
    private void validateRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                String.format("La calificación debe estar entre %d y %d estrellas.", MIN_RATING, MAX_RATING)
            );
        }
    }

    /**
     * Verifica si el usuario dado puede enviar feedback para la atención asociada.
     * El usuario debe ser el paciente o el practicante de la atención.
     *
     * @param user El usuario a verificar
     * @return true si el usuario puede enviar feedback, false en caso contrario
     */
    public boolean canUserSubmitFeedback(User user) {
        if (attention == null || user == null) {
            return false;
        }

        // Verificar si es el paciente
        Patient patient = attention.getPatient();
        if (patient != null && patient.getUser() != null
                && patient.getUser().getId().equals(user.getId())) {
            return true;
        }

        // Verificar si es el practicante
        Practitioner practitioner = attention.getPractitioner();
        if (practitioner != null && practitioner.getUser() != null
                && practitioner.getUser().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }

    // Getters y Setters

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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        validateRating(rating);
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