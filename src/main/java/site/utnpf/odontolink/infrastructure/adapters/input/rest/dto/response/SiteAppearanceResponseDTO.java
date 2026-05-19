package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;

import java.time.Instant;

/**
 * Respuesta del {@code GET /api/site-config/appearance} y del PUT admin.
 *
 * <p>{@code activeCustomTheme} se completa SOLO si {@code themeVariantId}
 * apunta a un custom theme activo. El embebido evita un segundo round-trip
 * del FE: el landing publico puede aplicar la paleta en la primera carga
 * sin volver a pedirla.
 *
 * <p>El tipo del embebido es {@link CustomThemeSummaryResponseDTO} (sin
 * {@code sourceCss}) porque el endpoint publico es consumido por todos los
 * visitantes anonimos del landing y el CSS crudo puede pesar varios KB. Lo
 * usa solo el panel admin via {@code GET /custom-themes/{id}}.
 */
@Schema(description = "Configuracion visual global del sitio.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteAppearanceResponseDTO {

    private String themeVariantId;
    private String fontPairId;
    private SiteDefaultMode defaultMode;
    private boolean allowUserOverride;
    private int version;
    /** Instante UTC del ultimo PUT exitoso. Util para mostrar "modificado hace X" en el panel. */
    private Instant updatedAt;
    /**
     * Embebido cuando {@code themeVariantId} matchea un slug de custom
     * theme activo. {@code null} si es un built-in. Omite {@code sourceCss}
     * para minimizar el payload del endpoint publico.
     */
    private CustomThemeSummaryResponseDTO activeCustomTheme;

    public SiteAppearanceResponseDTO() {
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CustomThemeSummaryResponseDTO getActiveCustomTheme() {
        return activeCustomTheme;
    }

    public void setActiveCustomTheme(CustomThemeSummaryResponseDTO activeCustomTheme) {
        this.activeCustomTheme = activeCustomTheme;
    }
}
