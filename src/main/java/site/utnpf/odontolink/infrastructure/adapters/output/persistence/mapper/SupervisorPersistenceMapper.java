package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SupervisorEntity;

/**
 * Mapper para convertir entre Supervisor (dominio) y SupervisorEntity (persistencia).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class SupervisorPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    public static Supervisor toDomain(SupervisorEntity entity) {
        if (entity == null) {
            return null;
        }

        Supervisor supervisor = new Supervisor();
        supervisor.setId(entity.getId());
        supervisor.setUser(UserPersistenceMapper.toDomain(entity.getUser()));
        supervisor.setSpecialty(entity.getSpecialty());
        supervisor.setEmployeeId(entity.getEmployeeId());

        return supervisor;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    public static SupervisorEntity toEntity(Supervisor supervisor) {
        if (supervisor == null) {
            return null;
        }

        SupervisorEntity entity = new SupervisorEntity();
        entity.setId(supervisor.getId());
        entity.setUser(UserPersistenceMapper.toEntity(supervisor.getUser()));
        entity.setSpecialty(supervisor.getSpecialty());
        entity.setEmployeeId(supervisor.getEmployeeId());

        return entity;
    }
}
