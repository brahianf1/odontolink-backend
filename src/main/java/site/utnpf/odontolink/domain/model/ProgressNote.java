package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Representa una "Evolución" o nota de progreso clínica.
 * Es una nota simple con fecha, que pertenece a una "Atención".
 */
public class ProgressNote {
    private Long id;

    /** Relación N-a-1: La "Atención" (caso) a la que pertenece esta nota */
    private Attention attention;

    private String note;
    private User author; // Quién escribió la nota (Practicante o Supervisor)
    private Instant createdAt;

    // Constructor usado por la Attention
    protected ProgressNote(Attention attention, String note, User author) {
        this.attention = attention;
        this.note = note;
        this.author = author;
        this.createdAt = Instant.now();
    }
}