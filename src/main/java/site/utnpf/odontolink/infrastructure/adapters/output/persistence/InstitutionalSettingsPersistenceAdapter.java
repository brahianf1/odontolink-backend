package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.InstitutionalSettings;
import site.utnpf.odontolink.domain.repository.InstitutionalSettingsRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.InstitutionalSettingsEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaInstitutionalSettingsRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.InstitutionalSettingsPersistenceMapper;

import java.util.Optional;

/**
 * Adaptador de persistencia para {@link InstitutionalSettings} (RF07).
 *
 * Implementa el puerto del dominio sobre JPA. Para preservar la naturaleza
 * singleton del agregado, todas las búsquedas se realizan contra el
 * identificador fijo {@link InstitutionalSettings#SINGLETON_ID}, y la
 * operación de guardado fuerza siempre ese mismo id antes de persistir
 * para evitar que una corrupción accidental cree filas paralelas.
 *
 * Politica transaccional uniforme con el resto de adapters; ver
 * {@link UserPersistenceAdapter} para el racional.
 */
@Component
@Transactional(readOnly = true)
public class InstitutionalSettingsPersistenceAdapter implements InstitutionalSettingsRepository {

    private final JpaInstitutionalSettingsRepository jpaRepository;

    public InstitutionalSettingsPersistenceAdapter(JpaInstitutionalSettingsRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<InstitutionalSettings> findSingleton() {
        return jpaRepository.findById(InstitutionalSettings.SINGLETON_ID)
                .map(InstitutionalSettingsPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public InstitutionalSettings save(InstitutionalSettings settings) {
        // Defensa en profundidad: aunque el dominio asigna el id por defecto,
        // re-forzamos el SINGLETON_ID aquí para garantizar que cualquier
        // futuro caller (incluido el servicio) no pueda romper la unicidad.
        settings.setId(InstitutionalSettings.SINGLETON_ID);

        InstitutionalSettingsEntity entity = InstitutionalSettingsPersistenceMapper.toEntity(settings);
        InstitutionalSettingsEntity savedEntity = jpaRepository.save(entity);
        return InstitutionalSettingsPersistenceMapper.toDomain(savedEntity);
    }
}
