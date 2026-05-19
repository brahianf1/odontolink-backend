package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import site.utnpf.odontolink.domain.model.CustomThemeTier;

import java.time.Instant;
import java.util.Map;

/**
 * Entidad JPA para la tabla {@code custom_themes} (RF-site-appearance).
 *
 * <p>Columnas {@code light_tokens} y {@code dark_tokens} usan el tipo
 * {@code JSON} nativo de MySQL 8, mapeado por Hibernate 6 via
 * {@link JdbcTypeCode}({@link SqlTypes#JSON}). Esto:
 * <ul>
 *   <li>Evita un {@code AttributeConverter} manual con Jackson.</li>
 *   <li>Permite queries internas a JSON si en el futuro las necesitamos
 *       (e.g. "themes que tengan {@code primary} verde").</li>
 *   <li>Persiste el {@code Map<String,String>} como JSON object real, no
 *       como texto opaco.</li>
 * </ul>
 *
 * <p>{@code source_css} usa {@code MEDIUMTEXT} (16 MB) en vez de {@code TEXT}
 * (64 KB) para tener margen sobre el limite de 50 KB que pide el FE: el
 * limite duro vive en la anotacion {@code @Size} del DTO, pero la columna
 * no tiene que estar al borde.
 *
 * <p>{@code deleted_at} implementa soft delete: las queries de listado y
 * lookup filtran {@code IS NULL}. No usamos {@code @SQLDelete} de Hibernate
 * porque queremos control explicito en cada consulta.
 */
@Entity
@Table(name = "custom_themes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_custom_themes_slug",
                columnNames = "slug"))
public class CustomThemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "mood", nullable = false, length = 120)
    private String mood;

    @Column(name = "fit_score", nullable = false)
    private int fitScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 16)
    private CustomThemeTier tier;

    @Column(name = "default_font_pair", nullable = false, length = 80)
    private String defaultFontPair;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "light_tokens", nullable = false, columnDefinition = "JSON")
    private Map<String, String> lightTokens;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dark_tokens", nullable = false, columnDefinition = "JSON")
    private Map<String, String> darkTokens;

    @Column(name = "source_css", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String sourceCss;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public CustomThemeEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public int getFitScore() {
        return fitScore;
    }

    public void setFitScore(int fitScore) {
        this.fitScore = fitScore;
    }

    public CustomThemeTier getTier() {
        return tier;
    }

    public void setTier(CustomThemeTier tier) {
        this.tier = tier;
    }

    public String getDefaultFontPair() {
        return defaultFontPair;
    }

    public void setDefaultFontPair(String defaultFontPair) {
        this.defaultFontPair = defaultFontPair;
    }

    public Map<String, String> getLightTokens() {
        return lightTokens;
    }

    public void setLightTokens(Map<String, String> lightTokens) {
        this.lightTokens = lightTokens;
    }

    public Map<String, String> getDarkTokens() {
        return darkTokens;
    }

    public void setDarkTokens(Map<String, String> darkTokens) {
        this.darkTokens = darkTokens;
    }

    public String getSourceCss() {
        return sourceCss;
    }

    public void setSourceCss(String sourceCss) {
        this.sourceCss = sourceCss;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
