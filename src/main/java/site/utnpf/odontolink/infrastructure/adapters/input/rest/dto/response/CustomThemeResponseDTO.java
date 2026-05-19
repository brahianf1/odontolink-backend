package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.CustomThemeTier;

import java.time.Instant;
import java.util.Map;

/**
 * Respuesta detallada de un custom theme. Se usa en:
 * <ul>
 *   <li>{@code GET /api/admin/site-config/custom-themes/{id}}</li>
 *   <li>{@code POST /api/admin/site-config/custom-themes} (creacion)</li>
 *   <li>{@code PUT /api/admin/site-config/custom-themes/{id}}</li>
 * </ul>
 *
 * <p>Para el listado se usa {@link CustomThemeSummaryResponseDTO}, que
 * omite {@code sourceCss} para no inflar el payload.
 */
@Schema(description = "Custom theme con paletas completas y source CSS.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomThemeResponseDTO {

    private Long id;
    private String slug;
    private String name;
    private String description;
    private String mood;
    private int fitScore;
    private CustomThemeTier tier;
    private String defaultFontPair;
    private Map<String, String> light;
    private Map<String, String> dark;
    private String sourceCss;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;

    public CustomThemeResponseDTO() {
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
