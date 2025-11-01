package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AvailabilitySlotEntity;

/**
 * Mapper para convertir entre AvailabilitySlot (dominio) y AvailabilitySlotEntity (persistencia).
 * 
 * Nota: No mapea la relación con OfferedTreatment para evitar ciclos.
 * La relación se establece mediante los métodos de utilidad en OfferedTreatment.
 */
public class AvailabilitySlotPersistenceMapper {

    /**
     * Convierte de dominio a entidad JPA (sin la relación con OfferedTreatment).
     */
    public static AvailabilitySlotEntity toEntity(AvailabilitySlot domain) {
        if (domain == null) {
            return null;
        }

        AvailabilitySlotEntity entity = new AvailabilitySlotEntity();
        entity.setId(domain.getId());
        entity.setDayOfWeek(domain.getDayOfWeek());
        entity.setStartTime(domain.getStartTime());
        entity.setEndTime(domain.getEndTime());

        return entity;
    }

    /**
     * Convierte de entidad JPA a dominio (sin la relación con OfferedTreatment).
     */
    public static AvailabilitySlot toDomain(AvailabilitySlotEntity entity) {
        if (entity == null) {
            return null;
        }

        AvailabilitySlot domain = new AvailabilitySlot();
        domain.setId(entity.getId());
        domain.setDayOfWeek(entity.getDayOfWeek());
        domain.setStartTime(entity.getStartTime());
        domain.setEndTime(entity.getEndTime());

        return domain;
    }
}
