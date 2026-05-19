package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.CustomTheme;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia de custom themes
 * (RF-site-appearance).
 *
 * <p>Todas las queries de lectura filtran {@code deletedAt IS NULL}: el
 * borrado es logico para conservar trazabilidad pero el catalogo activo NO
 * debe exponer themes eliminados.
 */
public interface CustomThemeRepository {

    /**
     * Themes activos ordenados por {@code createdAt DESC}. Excluye los
     * soft-deleted. Pensado para el listado del panel admin.
     */
    List<CustomTheme> findAllActive();

    Optional<CustomTheme> findActiveById(Long id);

    /**
     * Lookup por slug que el endpoint publico usa para decidir si el
     * {@code themeVariantId} del singleton apunta a un custom embebible.
     */
    Optional<CustomTheme> findActiveBySlug(String slug);

    /** Helper para validacion de colisiones durante autogeneracion de slug. */
    boolean existsActiveBySlug(String slug);

    CustomTheme save(CustomTheme theme);
}
