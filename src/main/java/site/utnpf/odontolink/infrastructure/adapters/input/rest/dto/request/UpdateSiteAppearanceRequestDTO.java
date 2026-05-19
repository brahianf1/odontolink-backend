package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;

/**
 * Cuerpo del {@code PUT /api/admin/site-config/appearance}. Contrato:
 * fotografia completa (no PATCH parcial) — el cliente envia los cuatro
 * campos editables. Si quiere conservar uno, lo reenvia tal cual.
 *
 * <p>Validacion {@code @Valid} cubre forma. La validacion semantica de
 * {@code themeVariantId} (existencia del slug cuando es custom) vive en el
 * servicio porque depende del repositorio.
 */
@Schema(description = "Carga completa para actualizar la appearance global del sitio.")
public class UpdateSiteAppearanceRequestDTO {

    @Schema(description = "Slug del theme. Built-in (e.g. 'theme-14') o custom (prefijo 'custom-').",
            example = "theme-14", required = true)
    @NotBlank(message = "El campo 'themeVariantId' es obligatorio")
    @Size(max = 80, message = "El 'themeVariantId' no puede superar los 80 caracteres")
    private String themeVariantId;

    @Schema(description = "Identificador del font pair del catalogo del frontend.",
            example = "inter-source-jetbrains", required = true)
    @NotBlank(message = "El campo 'fontPairId' es obligatorio")
    @Size(max = 80, message = "El 'fontPairId' no puede superar los 80 caracteres")
    private String fontPairId;

    @Schema(description = "Modo de color por defecto.",
            example = "SYSTEM", required = true,
            allowableValues = {"LIGHT", "DARK", "SYSTEM"})
    @NotNull(message = "El campo 'defaultMode' es obligatorio")
    private SiteDefaultMode defaultMode;

    @Schema(description = "Si el usuario final puede sobreescribir el modo en su navegador.",
            example = "false", required = true)
    @NotNull(message = "El campo 'allowUserOverride' es obligatorio")
    private Boolean allowUserOverride;

    public UpdateSiteAppearanceRequestDTO() {
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

    public Boolean getAllowUserOverride() {
        return allowUserOverride;
    }

    public void setAllowUserOverride(Boolean allowUserOverride) {
        this.allowUserOverride = allowUserOverride;
    }
}
