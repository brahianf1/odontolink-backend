package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import site.utnpf.odontolink.domain.model.FeedbackDirection;

import java.time.Instant;

/**
 * Entidad JPA del catálogo {@code feedback_criteria}.
 *
 * <p>El campo {@code code} es la clave estable del contrato con el frontend
 * y se enforcea como UK. El índice compuesto
 * {@code (applicable_direction, active, display_order)} sirve a la query
 * más caliente del módulo: la del catálogo que pinta el formulario.
 */
@Entity
@Table(name = "feedback_criteria",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_feedback_criterion_code", columnNames = {"code"})
        },
        indexes = {
            @Index(name = "idx_feedback_criterion_direction_active_order",
                    columnList = "applicable_direction,active,display_order")
        })
public class FeedbackCriterionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, updatable = false, length = 40)
    private String code;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_direction", nullable = false, length = 30)
    private FeedbackDirection applicableDirection;

    @Column(name = "include_in_ranking", nullable = false)
    private boolean includeInRanking;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    /**
     * Timestamp del último pasaje activo→inactivo. {@code null} si el
     * criterio nunca fue desactivado o si fue reactivado. Lo administra el
     * dominio ({@code FeedbackCriterion.setActive}); persistencia sólo lo
     * round-trippea.
     */
    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FeedbackCriterionEntity() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FeedbackDirection getApplicableDirection() {
        return applicableDirection;
    }

    public void setApplicableDirection(FeedbackDirection applicableDirection) {
        this.applicableDirection = applicableDirection;
    }

    public boolean isIncludeInRanking() {
        return includeInRanking;
    }

    public void setIncludeInRanking(boolean includeInRanking) {
        this.includeInRanking = includeInRanking;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(Instant deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
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
