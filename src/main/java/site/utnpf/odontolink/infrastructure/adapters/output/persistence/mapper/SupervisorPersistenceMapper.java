package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SupervisorEntity;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Supervisor (dominio) y SupervisorEntity (persistencia).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class SupervisorPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     * Mapea la relación N-a-N con practicantes supervisados.
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

        // Mapear relación N-a-N con practicantes (lazy loading safe)
        if (entity.getSupervisedPractitioners() != null) {
            supervisor.setSupervisedPractitioners(
                entity.getSupervisedPractitioners().stream()
                    .map(PractitionerPersistenceMapper::toDomain)
                    .collect(Collectors.toSet())
            );
        }

        return supervisor;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     * Mapea la relación N-a-N con practicantes supervisados.
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

        // Mapear relación N-a-N con practicantes
        if (supervisor.getSupervisedPractitioners() != null) {
            entity.setSupervisedPractitioners(
                supervisor.getSupervisedPractitioners().stream()
                    .map(PractitionerPersistenceMapper::toEntity)
                    .collect(Collectors.toSet())
            );
        } else {
            entity.setSupervisedPractitioners(new HashSet<>());
        }

        return entity;
    }
}
