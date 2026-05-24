package site.utnpf.odontolink.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Master-data inmutable-por-código que define un criterio sobre el que un
 * {@link Feedback} puede ser puntuado.
 *
 * <p>Soporta la encuesta multi-criterio del paciente (puntualidad, calidad de
 * atención, claridad en la comunicación, satisfacción general) y deja la
 * puerta abierta a sumar criterios sin migraciones de esquema: agregar uno
 * nuevo es insertar una fila más en {@code feedback_criteria}. El campo
 * {@code code} (machine-readable, en MAYÚSCULAS con guiones bajos) es el
 * contrato estable con el frontend y NO se permite modificar tras la
 * creación; los nombres visibles (display) sí evolucionan libremente.
 *
 * <p>{@link #applicableDirection} restringe en qué sentido del feedback
 * bidireccional aplica el criterio: la encuesta del paciente y la del
 * practicante usan sets distintos. {@link #includeInRanking} discrimina
 * además qué criterios participan del ranking combinado del panel docente
 * (satisfacción general se considera holística y no debe contaminar el
 * ranking comparativo entre practicantes).
 */
public class FeedbackCriterion {

    private Long id;
    private final String code;
    private String displayName;
    private String description;
    private FeedbackDirection applicableDirection;
    private boolean includeInRanking;
    private int displayOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Timestamp del momento en que el criterio fue desactivado. Lo administra
     * {@link #setActive(boolean)} automáticamente: se setea cuando pasa de
     * activo→inactivo y se nulea cuando reactivan. Sirve como registro de
     * auditoría y habilita (sin migración futura) queries "as-of date" sobre
     * la definición vigente al momento de un feedback. {@code null} si nunca
     * fue desactivado.
     */
    private Instant deactivatedAt;

    public FeedbackCriterion(String code,
                             String displayName,
                             String description,
                             FeedbackDirection applicableDirection,
                             boolean includeInRanking,
                             int displayOrder,
                             boolean active) {
        this.code = normalizeCode(code);
        this.displayName = requireNonBlank(displayName, "displayName");
        this.description = description;
        this.applicableDirection = Objects.requireNonNull(applicableDirection, "applicableDirection");
        this.includeInRanking = includeInRanking;
        this.displayOrder = displayOrder;
        this.active = active;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Constructor sin argumentos para uso interno de mappers de persistencia.
     * Inicializa {@code code} a {@code null}: los mappers deben usar el
     * constructor canónico y sólo recurrir a este si reflectan campo a campo.
     */
    protected FeedbackCriterion() {
        this.code = null;
    }

    private static String normalizeCode(String code) {
        String value = requireNonBlank(code, "code").trim().toUpperCase();
        if (!value.matches("^[A-Z][A-Z0-9_]{2,39}$")) {
            throw new IllegalArgumentException(
                    "code debe matchear ^[A-Z][A-Z0-9_]{2,39}$ — recibido: " + code);
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede ser null ni vacío.");
        }
        return value;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = requireNonBlank(displayName, "displayName");
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public FeedbackDirection getApplicableDirection() {
        return applicableDirection;
    }

    public void setApplicableDirection(FeedbackDirection applicableDirection) {
        this.applicableDirection = Objects.requireNonNull(applicableDirection, "applicableDirection");
        this.updatedAt = Instant.now();
    }

    public boolean isIncludeInRanking() {
        return includeInRanking;
    }

    public void setIncludeInRanking(boolean includeInRanking) {
        this.includeInRanking = includeInRanking;
        this.updatedAt = Instant.now();
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        boolean wasActive = this.active;
        this.active = active;
        Instant now = Instant.now();
        if (wasActive && !active) {
            this.deactivatedAt = now;
        } else if (!wasActive && active) {
            this.deactivatedAt = null;
        }
        this.updatedAt = now;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    /**
     * Setter de uso restringido a mappers de persistencia para rehidratar
     * el estado tal cual está en BD. La lógica de auto-management vive en
     * {@link #setActive(boolean)} y no debe ser bypasseada en código nuevo.
     */
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
