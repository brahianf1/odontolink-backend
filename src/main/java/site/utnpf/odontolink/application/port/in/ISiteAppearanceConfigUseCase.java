package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;

import java.util.Optional;

/**
 * Caso de uso para gestionar la configuracion visual global del sitio
 * (RF-site-appearance).
 *
 * <p>El metodo {@link #get()} no requiere permisos especiales: lo consume
 * tanto el panel admin como el landing publico. La autorizacion vive en el
 * filtro de seguridad por path, no aca.
 *
 * <p>{@link #update(UpdateAppearanceCommand, int, Long)} aplica optimistic
 * locking: el cliente envia la {@code version} que vio en su ultimo GET y
 * el servicio rechaza con {@code VersionConflictException} si esta stale.
 */
public interface ISiteAppearanceConfigUseCase {

    /**
     * Lee la appearance actual (siembra defaults si la fila no existia).
     * Si el {@code themeVariantId} apunta a un custom theme activo, el
     * embedded viene en {@link AppearanceSnapshot#activeCustomTheme()}.
     */
    AppearanceSnapshot get();

    AppearanceSnapshot update(UpdateAppearanceCommand command,
                              int expectedVersion,
                              Long actorUserId);

    /**
     * Snapshot inmutable devuelto al controller: el modelo de dominio mas el
     * custom theme embebido (si aplica). Evita que el controller tenga que
     * resolver el embedded por su cuenta y mantiene la atomicidad de
     * "appearance + custom" en una sola lectura del servicio.
     */
    record AppearanceSnapshot(SiteAppearanceConfig appearance,
                              Optional<CustomTheme> activeCustomTheme) {
    }

    record UpdateAppearanceCommand(String themeVariantId,
                                   String fontPairId,
                                   SiteDefaultMode defaultMode,
                                   boolean allowUserOverride) {
    }
}
