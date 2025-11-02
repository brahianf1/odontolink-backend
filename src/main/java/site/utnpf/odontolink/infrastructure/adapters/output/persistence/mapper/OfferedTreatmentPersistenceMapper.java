package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AvailabilitySlotEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.OfferedTreatmentEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre OfferedTreatment (dominio) y OfferedTreatmentEntity (persistencia).
 * Maneja la conversión completa incluyendo las relaciones con Practitioner, Treatment y AvailabilitySlots.
 */
public class OfferedTreatmentPersistenceMapper {

    /**
     * Convierte de dominio a entidad JPA.
     * Mapea todas las relaciones incluyendo los slots de disponibilidad.
     */
    public static OfferedTreatmentEntity toEntity(OfferedTreatment domain) {
        if (domain == null) {
            return null;
        }

        OfferedTreatmentEntity entity = new OfferedTreatmentEntity();
        entity.setId(domain.getId());
        entity.setRequirements(domain.getRequirements());
        entity.setDurationInMinutes(domain.getDurationInMinutes());

        // Mapear las relaciones
        if (domain.getPractitioner() != null) {
            entity.setPractitioner(PractitionerPersistenceMapper.toEntity(domain.getPractitioner()));
        }

        if (domain.getTreatment() != null) {
            entity.setTreatment(TreatmentPersistenceMapper.toEntity(domain.getTreatment()));
        }

        // Mapear los slots de disponibilidad
        if (domain.getAvailabilitySlots() != null) {
            Set<AvailabilitySlotEntity> slotEntities = domain.getAvailabilitySlots().stream()
                    .map(slot -> {
                        AvailabilitySlotEntity slotEntity = AvailabilitySlotPersistenceMapper.toEntity(slot);
                        slotEntity.setOfferedTreatment(entity); // Establecer la relación bidireccional
                        return slotEntity;
                    })
                    .collect(Collectors.toSet());
            entity.setAvailabilitySlots(slotEntities);
        }

        return entity;
    }

    /**
     * Convierte de entidad JPA a dominio.
     * Mapea todas las relaciones incluyendo los slots de disponibilidad.
     */
    public static OfferedTreatment toDomain(OfferedTreatmentEntity entity) {
        if (entity == null) {
            return null;
        }

        OfferedTreatment domain = new OfferedTreatment();
        domain.setId(entity.getId());
        domain.setRequirements(entity.getRequirements());
        domain.setDurationInMinutes(entity.getDurationInMinutes());

        // Mapear las relaciones
        if (entity.getPractitioner() != null) {
            domain.setPractitioner(PractitionerPersistenceMapper.toDomain(entity.getPractitioner()));
        }

        if (entity.getTreatment() != null) {
            domain.setTreatment(TreatmentPersistenceMapper.toDomain(entity.getTreatment()));
        }

        // Mapear los slots de disponibilidad
        if (entity.getAvailabilitySlots() != null) {
            Set<AvailabilitySlot> slots = entity.getAvailabilitySlots().stream()
                    .map(slotEntity -> {
                        AvailabilitySlot slot = AvailabilitySlotPersistenceMapper.toDomain(slotEntity);
                        slot.setOfferedTreatment(domain); // Establecer la relación bidireccional
                        return slot;
                    })
                    .collect(Collectors.toSet());
            domain.setAvailabilitySlots(slots);
        }

        return domain;
    }
}
