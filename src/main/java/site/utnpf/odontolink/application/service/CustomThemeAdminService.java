package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ICustomThemeAdminUseCase;
import site.utnpf.odontolink.application.service.support.SlugGenerator;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.ThemeInUseException;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.domain.repository.CustomThemeRepository;
import site.utnpf.odontolink.domain.repository.SiteAppearanceConfigRepository;

import java.util.List;

/**
 * Servicio de aplicacion para el CRUD de custom themes del catalogo
 * (RF-site-appearance).
 *
 * <p>Reglas no obvias:
 * <ul>
 *   <li>El {@code slug} se autogenera a partir del {@code name} con
 *       {@link SlugGenerator}, y se valida unicidad consultando el repo.
 *       Esto permite que el admin pueda enviar el mismo nombre dos veces
 *       (caso real: equipo experimentando) sin que falle con 400.</li>
 *   <li>Tama&ntilde;o de {@code sourceCss}: el DTO valida {@code @Size(max=51200)}
 *       (50 KB). La validacion adicional aca seria redundante; el dominio
 *       solo verifica que no este blank.</li>
 *   <li>Soft delete bloquea cuando el slug coincide con
 *       {@code themeVariantId} del singleton: evita dejar el sitio apuntando
 *       a un theme inexistente.</li>
 * </ul>
 */
@Transactional
public class CustomThemeAdminService implements ICustomThemeAdminUseCase {

    private final CustomThemeRepository repository;
    private final SiteAppearanceConfigRepository appearanceRepository;

    public CustomThemeAdminService(CustomThemeRepository repository,
                                   SiteAppearanceConfigRepository appearanceRepository) {
        this.repository = repository;
        this.appearanceRepository = appearanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomTheme> list() {
        return repository.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomTheme getById(Long id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomTheme", "id", String.valueOf(id)));
    }

    @Override
    public CustomTheme create(CreateCommand command, Long actorUserId) {
        // Generacion de slug ANTES de instanciar el dominio: el dominio
        // valida tokens y otros invariantes, pero el slug es una decision
        // de la capa de aplicacion (depende del estado actual del catalogo).
        String slug = SlugGenerator.generate(
                command.name(),
                repository::existsActiveBySlug
        );
        CustomTheme theme = CustomTheme.newTheme(
                slug,
                command.name(),
                command.description(),
                command.mood(),
                command.fitScore(),
                command.tier(),
                command.defaultFontPair(),
                command.light(),
                command.dark(),
                command.sourceCss(),
                actorUserId
        );
        return repository.save(theme);
    }

    @Override
    public CustomTheme update(Long id,
                              UpdateCommand command,
                              int expectedVersion,
                              Long actorUserId) {
        CustomTheme theme = repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomTheme", "id", String.valueOf(id)));
        // Optimistic locking: dos admins editando el mismo theme se evitan
        // pisar. Aborta antes de mutar.
        if (theme.getVersion() != expectedVersion) {
            throw new VersionConflictException(theme.getVersion());
        }
        theme.applyChanges(
                command.name(),
                command.description(),
                command.mood(),
                command.fitScore(),
                command.tier(),
                command.defaultFontPair(),
                command.light(),
                command.dark(),
                command.sourceCss(),
                actorUserId
        );
        return repository.save(theme);
    }

    @Override
    public void softDelete(Long id, Long actorUserId) {
        CustomTheme theme = repository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomTheme", "id", String.valueOf(id)));
        // Integridad referencial: no borrar si el theme esta apuntado por la
        // appearance activa. El singleton SIEMPRE existe en operacion normal
        // (lo siembra el bootstrap), asi que un Optional.empty aqui indica
        // un estado degradado en el que no hay nada que proteger.
        SiteAppearanceConfig singleton = appearanceRepository.findSingleton().orElse(null);
        if (singleton != null && theme.getSlug().equals(singleton.getThemeVariantId())) {
            throw new ThemeInUseException(theme.getSlug());
        }
        theme.markDeleted(actorUserId);
        repository.save(theme);
    }

    /**
     * Helper interno por si quisieramos exponer un check de unicidad del
     * name (no parte del contrato actual). Lo dejamos como referencia.
     */
    @SuppressWarnings("unused")
    private void assertSlugFree(String slug) {
        if (repository.existsActiveBySlug(slug)) {
            throw new InvalidBusinessRuleException(
                    "El slug '" + slug + "' ya esta en uso.");
        }
    }
}
