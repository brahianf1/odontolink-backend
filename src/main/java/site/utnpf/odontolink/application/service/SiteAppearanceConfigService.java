package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase;
import site.utnpf.odontolink.application.service.support.SingletonRowBootstrap;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.domain.repository.CustomThemeRepository;
import site.utnpf.odontolink.domain.repository.SiteAppearanceConfigRepository;

import java.util.Optional;

/**
 * Servicio de aplicacion para la configuracion visual global del sitio
 * (RF-site-appearance).
 *
 * <p>Estrategia "lazy bootstrap" igual que {@code InstitutionalSettingsService}:
 * la fila singleton se siembra con defaults la primera vez que alguien accede,
 * evitando 404s en el landing publico antes de que el admin entre por
 * primera vez al panel. El helper {@link SingletonRowBootstrap} garantiza
 * idempotencia bajo race conditions.
 *
 * <p>Optimistic locking via {@link VersionConflictException}: el cliente
 * envia la version observada (header {@code If-Match} en el adapter REST) y
 * el servicio la coteja contra el estado actual. Esto es nuevo en el
 * codebase y permite que dos admins editando concurrentemente no se pisen.
 */
@Transactional
public class SiteAppearanceConfigService implements ISiteAppearanceConfigUseCase {

    /**
     * Prefijo que distingue un custom theme de un built-in en el campo
     * {@code themeVariantId}. Si el slug arranca con este prefijo, el
     * servicio verifica que exista activo en {@link CustomThemeRepository}.
     */
    private static final String CUSTOM_THEME_PREFIX = "custom-";

    private final SiteAppearanceConfigRepository repository;
    private final CustomThemeRepository customThemeRepository;
    private final SingletonRowBootstrap bootstrap;

    public SiteAppearanceConfigService(SiteAppearanceConfigRepository repository,
                                       CustomThemeRepository customThemeRepository,
                                       SingletonRowBootstrap bootstrap) {
        this.repository = repository;
        this.customThemeRepository = customThemeRepository;
        this.bootstrap = bootstrap;
    }

    @Override
    @Transactional(readOnly = true)
    public AppearanceSnapshot get() {
        SiteAppearanceConfig config = bootstrap.getOrCreate(
                repository::findSingleton,
                SiteAppearanceConfig::defaults,
                repository::save,
                "SiteAppearanceConfig"
        );
        return buildSnapshot(config);
    }

    @Override
    public AppearanceSnapshot update(UpdateAppearanceCommand command,
                                     int expectedVersion,
                                     Long actorUserId) {
        // Reutilizamos get() para resolver el primer acceso del admin sin
        // que se rompa el flujo si la fila no existia antes del PUT.
        SiteAppearanceConfig current = bootstrap.getOrCreate(
                repository::findSingleton,
                SiteAppearanceConfig::defaults,
                repository::save,
                "SiteAppearanceConfig"
        );

        // Optimistic locking: si el cliente trae version vieja, abortamos
        // ANTES de tocar nada. El error lleva la version actual para que el
        // FE pueda recargar y reintentar sin un GET extra.
        if (current.getVersion() != expectedVersion) {
            throw new VersionConflictException(current.getVersion());
        }

        // Si el cliente apunta a un slug custom, verificamos que exista
        // activo. Para built-ins (sin prefijo) no validamos: el frontend
        // conoce el catalogo built-in y no queremos acoplarnos a esa lista.
        String requestedThemeId = command.themeVariantId() == null
                ? null : command.themeVariantId().trim();
        if (requestedThemeId != null && requestedThemeId.startsWith(CUSTOM_THEME_PREFIX)) {
            boolean exists = customThemeRepository.existsActiveBySlug(requestedThemeId);
            if (!exists) {
                throw new InvalidBusinessRuleException(
                        "El theme '" + requestedThemeId + "' no existe o fue eliminado.");
            }
        }

        current.applyChanges(
                requestedThemeId,
                command.fontPairId(),
                command.defaultMode(),
                command.allowUserOverride(),
                actorUserId
        );
        SiteAppearanceConfig saved = repository.save(current);
        return buildSnapshot(saved);
    }

    /**
     * Resuelve el embedded custom theme cuando el slug del singleton tiene
     * el prefijo {@code custom-}. Si el theme no se encuentra (e.g. fue
     * soft-deleted directamente en BD), el snapshot lo devuelve vacio y el
     * frontend caera a un default sin romper.
     */
    private AppearanceSnapshot buildSnapshot(SiteAppearanceConfig config) {
        Optional<CustomTheme> embedded = Optional.empty();
        String themeVariantId = config.getThemeVariantId();
        if (themeVariantId != null && themeVariantId.startsWith(CUSTOM_THEME_PREFIX)) {
            embedded = customThemeRepository.findActiveBySlug(themeVariantId);
        }
        return new AppearanceSnapshot(config, embedded);
    }

    // Exponemos un metodo dedicado para el getById usado por el panel admin
    // pero no por la API publica: deja el rol de "validar existencia" fuera
    // del controller. Lo dejamos comentado: no es parte del contrato actual.
    @SuppressWarnings("unused")
    private SiteAppearanceConfig requireSingleton() {
        return repository.findSingleton()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SiteAppearanceConfig", "id", String.valueOf(SiteAppearanceConfig.SINGLETON_ID)));
    }
}
