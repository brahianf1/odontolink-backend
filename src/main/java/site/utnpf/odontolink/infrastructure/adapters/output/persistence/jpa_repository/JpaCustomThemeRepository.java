package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.CustomThemeEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository para {@link CustomThemeEntity}.
 *
 * <p>Todas las queries de lectura filtran {@code deletedAt IS NULL} para
 * implementar soft delete sin {@code @SQLDelete}: el control queda visible
 * en el nombre de cada metodo y permite a tests/migraciones consultar
 * estado "borrado" cuando hace falta.
 */
@Repository
public interface JpaCustomThemeRepository
        extends JpaRepository<CustomThemeEntity, Long> {

    List<CustomThemeEntity> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<CustomThemeEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<CustomThemeEntity> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
