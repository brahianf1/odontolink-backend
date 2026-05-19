package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.repository.CustomThemeRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.CustomThemeEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaCustomThemeRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.CustomThemePersistenceMapper;

import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistencia para {@link CustomThemeRepository}.
 *
 * <p>Implementa el filtrado por {@code deleted_at IS NULL} en todas las
 * lecturas: el adapter es la frontera donde la decision "soft delete vs
 * hard delete" se vuelve concreta. El dominio no tiene por que saber que
 * existe un campo {@code deletedAt}; solo conoce "theme activo" / "theme
 * deleted".
 */
@Component
@Transactional(readOnly = true)
public class CustomThemePersistenceAdapter implements CustomThemeRepository {

    private final JpaCustomThemeRepository jpaRepository;

    public CustomThemePersistenceAdapter(JpaCustomThemeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<CustomTheme> findAllActive() {
        return jpaRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(CustomThemePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CustomTheme> findActiveById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(CustomThemePersistenceMapper::toDomain);
    }

    @Override
    public Optional<CustomTheme> findActiveBySlug(String slug) {
        return jpaRepository.findBySlugAndDeletedAtIsNull(slug)
                .map(CustomThemePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsActiveBySlug(String slug) {
        return jpaRepository.existsBySlugAndDeletedAtIsNull(slug);
    }

    @Override
    @Transactional
    public CustomTheme save(CustomTheme theme) {
        CustomThemeEntity entity = CustomThemePersistenceMapper.toEntity(theme);
        CustomThemeEntity saved = jpaRepository.save(entity);
        return CustomThemePersistenceMapper.toDomain(saved);
    }
}
