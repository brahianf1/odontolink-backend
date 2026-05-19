package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SiteAppearanceConfigEntity;

/**
 * Spring Data JPA repository de {@link SiteAppearanceConfigEntity}. La
 * naturaleza singleton del agregado vive en el adapter
 * ({@code SiteAppearanceConfigPersistenceAdapter}), aca solo exponemos
 * {@code findById(1L)} y {@code save()} via la interfaz heredada.
 */
@Repository
public interface JpaSiteAppearanceConfigRepository
        extends JpaRepository<SiteAppearanceConfigEntity, Long> {
}
