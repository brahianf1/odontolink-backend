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
 * Cuerpo del {@code PUT /api/admin/site-config/custom-themes/{id}}. Mismo
 * shape que el de creacion: el contrato es "reemplazo total" para mantener
 * la simetria con el resto de PUTs del codebase.
 *
 * <p>Se mantiene una clase separada (no se reusa
 * {@code CreateCustomThemeRequestDTO}) para que la documentacion Swagger
 * de cada endpoint diga literalmente "esto es lo que recibe" y para
 * permitir divergencias futuras (e.g. campos read-only del create) sin
 * que sea un breaking change.
 */
@Schema(description = "Actualizacion completa de un custom theme.")
public class UpdateCustomThemeRequestDTO {

    @NotBlank(message = "El campo 'name' es obligatorio")
    @Size(min = 3, max = 120, message = "'name' debe tener entre 3 y 120 caracteres")
    private String name;

    @Size(max = 500, message = "'description' no puede superar los 500 caracteres")
    private String description;

    @Size(max = 120, message = "'mood' no puede superar los 120 caracteres")
    private String mood;

    @NotNull(message = "El campo 'fitScore' es obligatorio")
    @Min(value = 1, message = "'fitScore' debe ser >= 1")
    @Max(value = 5, message = "'fitScore' debe ser <= 5")
    private Integer fitScore;

    @NotNull(message = "El campo 'tier' es obligatorio")
    private CustomThemeTier tier;

    @NotBlank(message = "El campo 'defaultFontPair' es obligatorio")
    @Size(max = 80, message = "'defaultFontPair' no puede superar los 80 caracteres")
    private String defaultFontPair;

    @NotEmpty(message = "El campo 'light' es obligatorio")
    private Map<String, String> light;

    @NotEmpty(message = "El campo 'dark' es obligatorio")
    private Map<String, String> dark;

    @NotBlank(message = "El campo 'sourceCss' es obligatorio")
    @Size(max = 51200, message = "'sourceCss' no puede superar los 50 KB")
    private String sourceCss;

    public UpdateCustomThemeRequestDTO() {
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
