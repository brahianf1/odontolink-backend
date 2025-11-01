package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;

/**
 * Mapper para convertir entre Treatment (dominio) y TreatmentEntity (persistencia).
 * Sigue el patrón de mapeo manual usado en el proyecto.
 */
public class TreatmentPersistenceMapper {

    /**
     * Convierte de dominio a entidad JPA.
     */
    public static TreatmentEntity toEntity(Treatment domain) {
        if (domain == null) {
            return null;
        }

        TreatmentEntity entity = new TreatmentEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setArea(domain.getArea());

        return entity;
    }

    /**
     * Convierte de entidad JPA a dominio.
     */
    public static Treatment toDomain(TreatmentEntity entity) {
        if (entity == null) {
            return null;
        }

        Treatment domain = new Treatment();
        domain.setId(entity.getId());
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setArea(entity.getArea());

        return domain;
    }
}
