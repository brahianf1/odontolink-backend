package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;

/**
 * Mapper para convertir entre Patient (dominio) y PatientEntity (persistencia).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class PatientPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    public static Patient toDomain(PatientEntity entity) {
        if (entity == null) {
            return null;
        }

        Patient patient = new Patient();
        patient.setId(entity.getId());
        patient.setUser(UserPersistenceMapper.toDomain(entity.getUser()));
        patient.setHealthInsurance(entity.getHealthInsurance());
        patient.setBloodType(entity.getBloodType());

        return patient;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    public static PatientEntity toEntity(Patient patient) {
        if (patient == null) {
            return null;
        }

        PatientEntity entity = new PatientEntity();
        entity.setId(patient.getId());
        entity.setUser(UserPersistenceMapper.toEntity(patient.getUser()));
        entity.setHealthInsurance(patient.getHealthInsurance());
        entity.setBloodType(patient.getBloodType());

        return entity;
    }
}
