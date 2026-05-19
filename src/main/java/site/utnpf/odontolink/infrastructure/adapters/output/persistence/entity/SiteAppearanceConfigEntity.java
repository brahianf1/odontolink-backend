package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code site_appearance_config}
 * (RF-site-appearance).
 *
 * <p>Sigue el patron de {@code InstitutionalSettingsEntity}: el id es fijo
 * ({@code 1L}, asignado en el adapter) en lugar de autogenerado, para
 * imponer la naturaleza singleton del agregado a nivel de modelo. La unica
 * fila tiene siempre {@code id = 1}; cualquier intento de crear una segunda
 * fila fallaria por PK duplicada.
 *
 * <p>El campo {@code version} (entero, no {@code @Version} de JPA) es el
 * contador de optimistic locking expuesto al cliente via {@code If-Match} /
 * {@code ETag}. Se maneja explicitamente en el dominio, NO se delega al
 * mecanismo de JPA porque queremos visibilidad explicita del invariante.
 */
@Entity
@Table(name = "site_appearance_config")
public class SiteAppearanceConfigEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "theme_variant_id", nullable = false, length = 80)
    private String themeVariantId;

    @Column(name = "font_pair_id", nullable = false, length = 80)
    private String fontPairId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_mode", nullable = false, length = 16)
    private SiteDefaultMode defaultMode;

    @Column(name = "allow_user_override", nullable = false)
    private boolean allowUserOverride;

    @Column(name = "version", nullable = false)
    private int version;

    /**
     * FK logica a {@code users.id}. Nullable porque la primera siembra
     * (defaults) no tiene actor humano detras. No declaramos constraint
     * fisico siguiendo la convencion del codebase para FKs auxiliares.
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SiteAppearanceConfigEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getThemeVariantId() {
        return themeVariantId;
    }

    public void setThemeVariantId(String themeVariantId) {
        this.themeVariantId = themeVariantId;
    }

    public String getFontPairId() {
        return fontPairId;
    }

    public void setFontPairId(String fontPairId) {
        this.fontPairId = fontPairId;
    }

    public SiteDefaultMode getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(SiteDefaultMode defaultMode) {
        this.defaultMode = defaultMode;
    }

    public boolean isAllowUserOverride() {
        return allowUserOverride;
    }

    public void setAllowUserOverride(boolean allowUserOverride) {
        this.allowUserOverride = allowUserOverride;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
}
