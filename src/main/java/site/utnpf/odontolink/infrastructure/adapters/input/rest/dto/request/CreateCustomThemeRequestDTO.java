package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import site.utnpf.odontolink.domain.model.CustomThemeTier;

import java.util.Map;

/**
 * Cuerpo del {@code POST /api/admin/site-config/custom-themes}.
 *
 * <p>Las anotaciones {@code @Valid} cubren forma (longitudes, rango, no
 * null). La validacion del contenido de los maps {@code light} y
 * {@code dark} (35 keys obligatorias, valores hex) la hace el servicio
 * usando {@code ThemeTokenContract}, porque {@code jakarta.validation} no
 * tiene una forma limpia de validar el contenido de un {@code Map}
 * dinamico.
 */
@Schema(description = "Alta de un custom theme.")
public class CreateCustomThemeRequestDTO {

    @Schema(example = "Clinica 2024", required = true)
    @NotBlank(message = "El campo 'name' es obligatorio")
    @Size(min = 3, max = 120, message = "'name' debe tener entre 3 y 120 caracteres")
    private String name;

    @Schema(example = "Theme corporativo")
    @Size(max = 500, message = "'description' no puede superar los 500 caracteres")
    private String description;

    @Schema(example = "Healthcare clean")
    @Size(max = 120, message = "'mood' no puede superar los 120 caracteres")
    private String mood;

    @Schema(example = "5", required = true)
    @NotNull(message = "El campo 'fitScore' es obligatorio")
    @Min(value = 1, message = "'fitScore' debe ser >= 1")
    @Max(value = 5, message = "'fitScore' debe ser <= 5")
    private Integer fitScore;

    @Schema(example = "OFFICIAL", required = true, allowableValues = {"OFFICIAL", "EXPERIMENTAL"})
    @NotNull(message = "El campo 'tier' es obligatorio")
    private CustomThemeTier tier;

    @Schema(example = "inter-source-jetbrains", required = true)
    @NotBlank(message = "El campo 'defaultFontPair' es obligatorio")
    @Size(max = 80, message = "'defaultFontPair' no puede superar los 80 caracteres")
    private String defaultFontPair;

    @Schema(description = "Map de 35 tokens (clave -> #rrggbb) para el modo claro.", required = true)
    @NotEmpty(message = "El campo 'light' es obligatorio")
    private Map<String, String> light;

    @Schema(description = "Map de 35 tokens (clave -> #rrggbb) para el modo oscuro.", required = true)
    @NotEmpty(message = "El campo 'dark' es obligatorio")
    private Map<String, String> dark;

    @Schema(description = "Bloque CSS shadcn-style pegado por el admin.", required = true)
    @NotBlank(message = "El campo 'sourceCss' es obligatorio")
    @Size(max = 51200, message = "'sourceCss' no puede superar los 50 KB")
    private String sourceCss;

    public CreateCustomThemeRequestDTO() {
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

    public Integer getFitScore() {
        return fitScore;
    }

    public void setFitScore(Integer fitScore) {
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

    public Map<String, String> getLight() {
        return light;
    }

    public void setLight(Map<String, String> light) {
        this.light = light;
    }

    public Map<String, String> getDark() {
        return dark;
    }

    public void setDark(Map<String, String> dark) {
        this.dark = dark;
    }

    public String getSourceCss() {
        return sourceCss;
    }

    public void setSourceCss(String sourceCss) {
        this.sourceCss = sourceCss;
    }
}
