package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.domain.repository.SiteAppearanceConfigRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SiteAppearanceConfigEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaSiteAppearanceConfigRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.SiteAppearanceConfigPersistenceMapper;

import java.util.Optional;

/**
 * Adapter de persistencia del puerto
 * {@link SiteAppearanceConfigRepository}.
 *
 * <p>Patron heredado de {@code InstitutionalSettingsPersistenceAdapter}: el
 * adapter es la unica frontera que conoce el {@code SINGLETON_ID} a nivel
 * de fila. {@code save()} fuerza siempre el id correcto antes de delegar,
 * lo que protege contra un caller despistado intentando crear filas
 * paralelas.
 */
@Component
@Transactional(readOnly = true)
public class SiteAppearanceConfigPersistenceAdapter implements SiteAppearanceConfigRepository {

    private final JpaSiteAppearanceConfigRepository jpaRepository;

    public SiteAppearanceConfigPersistenceAdapter(JpaSiteAppearanceConfigRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<SiteAppearanceConfig> findSingleton() {
        return jpaRepository.findById(SiteAppearanceConfig.SINGLETON_ID)
                .map(SiteAppearanceConfigPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public SiteAppearanceConfig save(SiteAppearanceConfig config) {
        // Defensa en profundidad: el id se fuerza aca para impedir crear una
        // segunda fila aunque el dominio venga con un id distinto.
        config.setId(SiteAppearanceConfig.SINGLETON_ID);
        SiteAppearanceConfigEntity entity = SiteAppearanceConfigPersistenceMapper.toEntity(config);
        SiteAppearanceConfigEntity saved = jpaRepository.save(entity);
        return SiteAppearanceConfigPersistenceMapper.toDomain(saved);
    }
}
