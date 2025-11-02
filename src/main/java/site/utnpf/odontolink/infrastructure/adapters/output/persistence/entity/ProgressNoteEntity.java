package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entidad JPA para la tabla 'progress_notes'.
 * Representa una "Nota de Progreso" o "Evolución" clínica en la base de datos.
 *
 * Esta entidad PERTENECE a una AttentionEntity (relación ManyToOne).
 * No tiene sentido sin su Attention padre.
 */
@Entity
@Table(name = "progress_notes")
public class ProgressNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación ManyToOne con AttentionEntity (bidireccional).
     *
     * JoinColumn indica que esta tabla tiene la FK hacia 'attentions'.
     * Este es el lado "owner" de la relación bidireccional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attention_id", nullable = false)
    private AttentionEntity attention;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    /**
     * Relación ManyToOne con UserEntity (autor de la nota).
     * Puede ser un Practicante o un Supervisor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(nullable = false)
    private Instant createdAt;

    // Constructores
    public ProgressNoteEntity() {
    }

    // Hook para establecer automáticamente la fecha de creación
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UserEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserEntity author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
