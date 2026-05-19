package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.model.CustomThemeTier;

import java.util.List;
import java.util.Map;

/**
 * Caso de uso CRUD para custom themes del catalogo (RF-site-appearance).
 *
 * <p>Todas las operaciones requieren rol ADMIN (defendido por SecurityConfig
 * + @PreAuthorize en el controller). El soft delete protege la integridad
 * referencial con {@link ISiteAppearanceConfigUseCase}: no permite eliminar
 * un theme en uso como appearance activa.
 */
public interface ICustomThemeAdminUseCase {

    /** Listado activo, ordenado por {@code createdAt DESC}. */
    List<CustomTheme> list();

    CustomTheme getById(Long id);

    CustomTheme create(CreateCommand command, Long actorUserId);

    CustomTheme update(Long id,
                       UpdateCommand command,
                       int expectedVersion,
                       Long actorUserId);

    /**
     * Soft delete. Falla con {@code ThemeInUseException} si el slug del
     * theme coincide con el {@code themeVariantId} del singleton.
     */
    void softDelete(Long id, Long actorUserId);

    record CreateCommand(String name,
                         String description,
                         String mood,
                         int fitScore,
                         CustomThemeTier tier,
                         String defaultFontPair,
                         Map<String, String> light,
                         Map<String, String> dark,
                         String sourceCss) {
    }

    record UpdateCommand(String name,
                         String description,
                         String mood,
                         int fitScore,
                         CustomThemeTier tier,
                         String defaultFontPair,
                         Map<String, String> light,
                         Map<String, String> dark,
                         String sourceCss) {
    }
}
