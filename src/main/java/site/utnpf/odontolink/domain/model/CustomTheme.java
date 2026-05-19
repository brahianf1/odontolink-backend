package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.theme.ThemeTokenContract;

import java.time.Instant;
import java.util.Map;

/**
 * Theme personalizado subido por un admin a partir de CSS shadcn-style.
 *
 * <p>El admin pega un bloque CSS y, en paralelo, envia los 35 tokens
 * pre-extraidos para light y dark (el parseo lo hace el frontend porque
 * tiene visibilidad del tooling de shadcn). El backend solo se asegura de
 * que la forma sea valida y que el catalogo quede consistente.
 *
 * <p>Invariantes asegurados aca:
 * <ul>
 *   <li>{@code fitScore} en [1..5].</li>
 *   <li>{@code lightTokens} y {@code darkTokens} contienen exactamente las
 *       35 keys del {@link ThemeTokenContract} y cada value es hex.</li>
 *   <li>{@code slug} unico (validado a nivel persistencia + autogeneracion
 *       con sufijo si colisiona, hecho en el servicio).</li>
 * </ul>
 *
 * <p>Soft delete via {@code deletedAt}: el theme no se elimina fisicamente
 * para preservar trazabilidad de que tema se usaba historicamente, y porque
 * el slug podria volver a referenciarse en un audit/log. La query del
 * listado filtra {@code deletedAt IS NULL}.
 */
public class CustomTheme {

    private Long id;
    private String slug;
    private String name;
    private String description;
    private String mood;
    private int fitScore;
    private CustomThemeTier tier;
    private String defaultFontPair;
    private Map<String, String> lightTokens;
    private Map<String, String> darkTokens;
    private String sourceCss;
    private int version;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
    /** {@code null} si el theme esta activo; {@link Instant} de borrado si soft-deleted. */
    private Instant deletedAt;

    public CustomTheme() {
    }

    public CustomTheme(Long id,
                       String slug,
                       String name,
                       String description,
                       String mood,
                       int fitScore,
                       CustomThemeTier tier,
                       String defaultFontPair,
                       Map<String, String> lightTokens,
                       Map<String, String> darkTokens,
                       String sourceCss,
                       int version,
                       Long createdBy,
                       Instant createdAt,
                       Long updatedBy,
                       Instant updatedAt,
                       Instant deletedAt) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.mood = mood;
        this.fitScore = fitScore;
        this.tier = tier;
        this.defaultFontPair = defaultFontPair;
        this.lightTokens = lightTokens;
        this.darkTokens = darkTokens;
        this.sourceCss = sourceCss;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory para construir un theme nuevo desde un comando del servicio.
     * Aplica las validaciones de dominio y deja el agregado listo para
     * persistir con {@code id} y timestamps fijados por la capa de
     * infraestructura.
     */
    public static CustomTheme newTheme(String slug,
                                       String name,
                                       String description,
                                       String mood,
                                       int fitScore,
                                       CustomThemeTier tier,
                                       String defaultFontPair,
                                       Map<String, String> lightTokens,
                                       Map<String, String> darkTokens,
                                       String sourceCss,
                                       Long createdBy) {
        validateInputs(name, fitScore, tier, defaultFontPair, lightTokens, darkTokens, sourceCss);
        Instant now = Instant.now();
        return new CustomTheme(
                null,
                slug,
                name.trim(),
                description == null ? "" : description.trim(),
                mood == null ? "" : mood.trim(),
                fitScore,
                tier,
                defaultFontPair.trim(),
                lightTokens,
                darkTokens,
                sourceCss,
                1,
                createdBy,
                now,
                null,
                now,
                null
        );
    }

    /**
     * Reemplaza atomicamente todos los campos editables, incrementa version
     * y refresca {@code updatedBy}/{@code updatedAt}. NO permite mutar
     * {@code slug}: si el admin quiere otro slug, crea un theme nuevo
     * (decision del plan).
     */
    public void applyChanges(String name,
                             String description,
                             String mood,
                             int fitScore,
                             CustomThemeTier tier,
                             String defaultFontPair,
                             Map<String, String> lightTokens,
                             Map<String, String> darkTokens,
                             String sourceCss,
                             Long actorUserId) {
        validateInputs(name, fitScore, tier, defaultFontPair, lightTokens, darkTokens, sourceCss);
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.mood = mood == null ? "" : mood.trim();
        this.fitScore = fitScore;
        this.tier = tier;
        this.defaultFontPair = defaultFontPair.trim();
        this.lightTokens = lightTokens;
        this.darkTokens = darkTokens;
        this.sourceCss = sourceCss;
        this.version += 1;
        this.updatedBy = actorUserId;
        this.updatedAt = Instant.now();
    }

    /**
     * Marca el theme como eliminado logicamente. No tira excepcion si ya
     * estaba marcado (idempotente): facilita reintentos del cliente sin
     * tener que distinguir 204 de 404.
     */
    public void markDeleted(Long actorUserId) {
        if (this.deletedAt != null) {
            return;
        }
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedBy = actorUserId;
        this.updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static void validateInputs(String name,
                                       int fitScore,
                                       CustomThemeTier tier,
                                       String defaultFontPair,
                                       Map<String, String> lightTokens,
                                       Map<String, String> darkTokens,
                                       String sourceCss) {
        if (name == null || name.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'name' es obligatorio.");
        }
        if (fitScore < 1 || fitScore > 5) {
            throw new InvalidBusinessRuleException("'fitScore' debe estar en el rango [1, 5].");
        }
        if (tier == null) {
            throw new InvalidBusinessRuleException("El campo 'tier' es obligatorio.");
        }
        if (defaultFontPair == null || defaultFontPair.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'defaultFontPair' es obligatorio.");
        }
        if (sourceCss == null || sourceCss.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'sourceCss' es obligatorio.");
        }
        // El contrato de tokens valida ausencias y formato; reportar light y
        // dark por separado ayuda al admin a localizar el problema sin tener
        // que revisar dos paletas a la vez.
        ThemeTokenContract.validate(lightTokens, "light");
        ThemeTokenContract.validate(darkTokens, "dark");
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
