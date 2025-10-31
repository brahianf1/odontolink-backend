package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Representa el Feedback (calificación) sobre una Atención.
 * Modela tanto "Calificar Paciente" como "Calificar Practicante".
 */
public class Feedback {
    private Long id;

    /** Relación N-a-1: El feedback es SIEMPRE sobre un caso/atención */
    private Attention attention;

    /** Relación N-a-1: El User que escribió este feedback */
    private User submittedBy;

    private int rating; // 1 a 5 estrellas
    private String comment;
    private Instant createdAt;
}