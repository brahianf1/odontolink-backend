package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.InstitutionalSettings;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.InstitutionalSettingsEntity;

/**
 * Mapper estático entre {@link InstitutionalSettings} (dominio) y
 * {@link InstitutionalSettingsEntity} (persistencia).
 *
 * Siguiendo el patrón establecido en el resto del proyecto, el mapper se
 * mantiene como utilidad sin estado: las conversiones son simétricas y
 * pueden invocarse desde cualquier capa de infraestructura sin necesidad
 * de inyección.
 */
public final class InstitutionalSettingsPersistenceMapper {

    private InstitutionalSettingsPersistenceMapper() {
        // Clase de utilidades: no se instancia.
    }

    public static InstitutionalSettings toDomain(InstitutionalSettingsEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InstitutionalSettings(
                entity.getId(),
                entity.getInstitutionName(),
                entity.getOpeningHours(),
                entity.getUsagePolicies(),
                entity.getContactEmail(),
                entity.getContactPhone(),
                entity.getContactAddress(),
                entity.getUpdatedAt()
        );
    }

    public static InstitutionalSettingsEntity toEntity(InstitutionalSettings domain) {
        if (domain == null) {
            return null;
        }
        InstitutionalSettingsEntity entity = new InstitutionalSettingsEntity();
        entity.setId(domain.getId());
        entity.setInstitutionName(domain.getInstitutionName());
        entity.setOpeningHours(domain.getOpeningHours());
        entity.setUsagePolicies(domain.getUsagePolicies());
        entity.setContactEmail(domain.getContactEmail());
        entity.setContactPhone(domain.getContactPhone());
        entity.setContactAddress(domain.getContactAddress());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
