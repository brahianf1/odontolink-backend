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

    // Constructor sin argumentos (requerido por mappers de persistencia)
    public ProgressNote() {
    }

    // Constructor usado por la Attention
    protected ProgressNote(Attention attention, String note, User author) {
        this.attention = attention;
        this.note = note;
        this.author = author;
        this.createdAt = Instant.now();
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}