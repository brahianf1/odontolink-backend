package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;

/**
 * Configuracion visual global del sitio (RF-site-appearance).
 *
 * <p>Singleton: existe a lo sumo una fila identificada por {@link #SINGLETON_ID}.
 * La eleccion de modelarla como agregado tipado (en vez de tabla key-value)
 * sigue el patron de {@link InstitutionalSettings} y {@link AiAgentConfiguration}:
 * tipado fuerte, lectura unica para construir tanto el landing publico como
 * el panel admin, y una sola fuente de verdad.
 *
 * <p>Lleva {@code version} entero monotono creciente que el cliente debe
 * enviar en el header {@code If-Match} al hacer PUT. Es nuestro primer uso de
 * optimistic locking expuesto a HTTP, ya que dos admins editando en paralelo
 * podrian pisarse sin darse cuenta. El campo lo administra el dominio:
 * {@link #applyChanges(String, String, SiteDefaultMode, boolean, Long)}
 * incrementa {@code version} y refresca {@code updatedAt}/{@code updatedBy}
 * en la misma operacion.
 *
 * <p>El campo {@code themeVariantId} puede apuntar a:
 * <ul>
 *   <li>Un slug de built-in (e.g. {@code "theme-14"}, conocido por el
 *       frontend pero no validado contra catalogo aca).</li>
 *   <li>Un slug de custom theme (e.g. {@code "custom-clinic-2024"}). Si tiene
 *       prefijo {@code custom-}, el servicio verifica que exista y no este
 *       soft-deleted antes de aceptar el cambio.</li>
 * </ul>
 *
 * <p>NO embebemos validacion de existencia de slug aca porque el dominio no
 * conoce el repositorio de custom themes — esa coordinacion vive en el
 * servicio de aplicacion.
 */
public class SiteAppearanceConfig {

    /** Identificador unico de la fila singleton. Asignado por la app, no por la BD. */
    public static final Long SINGLETON_ID = 1L;

    /**
     * Slug del theme built-in que arranca el sitio si nadie lo cambia. El
     * frontend conoce esta etiqueta y la mapea a una paleta hardcodeada.
     */
    public static final String DEFAULT_THEME_VARIANT_ID = "theme-14";

    /** Font pair por defecto (Inter para body, JetBrains Mono para code). */
    public static final String DEFAULT_FONT_PAIR_ID = "inter-source-jetbrains";

    private Long id;
    private String themeVariantId;
    private String fontPairId;
    private SiteDefaultMode defaultMode;
    private boolean allowUserOverride;
    /**
     * Numero entero monotono que arranca en 1 y se incrementa con cada
     * {@link #applyChanges}. Es el contrato de optimistic locking con el
     * cliente.
     */
    private int version;
    /** {@code userId} del admin que aplico el ultimo cambio; {@code null} si nunca se edito. */
    private Long updatedBy;
    private Instant updatedAt;

    public SiteAppearanceConfig() {
    }

    public SiteAppearanceConfig(Long id,
                                String themeVariantId,
                                String fontPairId,
                                SiteDefaultMode defaultMode,
                                boolean allowUserOverride,
                                int version,
                                Long updatedBy,
                                Instant updatedAt) {
        this.id = id;
        this.themeVariantId = themeVariantId;
        this.fontPairId = fontPairId;
        this.defaultMode = defaultMode;
        this.allowUserOverride = allowUserOverride;
        this.version = version;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    /**
     * Defaults sembrados al primer acceso (via {@code SingletonRowBootstrap}).
     * Sin esta semilla, el landing publico recibiria un 404 antes de que el
     * admin pase por el panel — inaceptable porque el landing carga sin
     * sesion. Por eso los defaults viven en el dominio y son neutros.
     */
    public static SiteAppearanceConfig defaults() {
        return new SiteAppearanceConfig(
                SINGLETON_ID,
                DEFAULT_THEME_VARIANT_ID,
                DEFAULT_FONT_PAIR_ID,
                SiteDefaultMode.SYSTEM,
                false,
                1,
                null,
                Instant.now()
        );
    }

    /**
     * Aplica el set completo de cambios y avanza la version. Recibir todos
     * los campos en una sola operacion evita ambigüedades de PATCH parcial:
     * el cliente siempre envia la fotografia completa y nosotros respondemos
     * con la nueva fotografia + nuevo ETag.
     */
    public void applyChanges(String themeVariantId,
                             String fontPairId,
                             SiteDefaultMode defaultMode,
                             boolean allowUserOverride,
                             Long actorUserId) {
        validateNonBlank(themeVariantId, "themeVariantId");
        validateNonBlank(fontPairId, "fontPairId");
        if (defaultMode == null) {
            throw new InvalidBusinessRuleException("El campo 'defaultMode' es obligatorio.");
        }
        this.themeVariantId = themeVariantId.trim();
        this.fontPairId = fontPairId.trim();
        this.defaultMode = defaultMode;
        this.allowUserOverride = allowUserOverride;
        this.version += 1;
        this.updatedBy = actorUserId;
        this.updatedAt = Instant.now();
    }

    private static void validateNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidBusinessRuleException("El campo '" + field + "' es obligatorio.");
        }
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
