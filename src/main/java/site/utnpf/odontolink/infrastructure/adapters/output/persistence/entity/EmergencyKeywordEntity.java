package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_emergency_keywords} (RF32).
 *
 * <p>Unicidad case-insensitive: MySQL con collation por defecto
 * (utf8mb4_unicode_ci en versiones modernas) ya hace el unique sin acentos
 * y sin distinguir mayusculas. La unicidad normalizada (sin acentos) la
 * refuerza el servicio antes de guardar.
 */
@Entity
@Table(
        name = "ai_emergency_keywords",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_emergency_kw_term", columnNames = {"term"})
        }
)
public class EmergencyKeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "term", nullable = false, length = 100)
    private String term;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EmergencyKeywordEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
