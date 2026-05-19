package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;

/**
 * Respuesta del {@code GET /api/site-config/appearance} y del PUT admin.
 *
 * <p>{@code activeCustomTheme} se completa SOLO si {@code themeVariantId}
 * apunta a un custom theme activo. El embebido evita un segundo round-trip
 * del FE: el landing publico puede aplicar la paleta en la primera carga
 * sin volver a pedirla.
 */
@Schema(description = "Configuracion visual global del sitio.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteAppearanceResponseDTO {

    private String themeVariantId;
    private String fontPairId;
    private SiteDefaultMode defaultMode;
    private boolean allowUserOverride;
    private int version;
    /**
     * Embebido cuando {@code themeVariantId} matchea un slug de custom
     * theme activo. {@code null} si es un built-in.
     */
    private CustomThemeResponseDTO activeCustomTheme;

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

    public CustomThemeResponseDTO getActiveCustomTheme() {
        return activeCustomTheme;
    }

    public void setActiveCustomTheme(CustomThemeResponseDTO activeCustomTheme) {
        this.activeCustomTheme = activeCustomTheme;
    }
}
