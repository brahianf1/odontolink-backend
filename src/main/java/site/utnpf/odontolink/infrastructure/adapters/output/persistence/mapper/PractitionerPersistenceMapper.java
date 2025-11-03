package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Practitioner (dominio) y PractitionerEntity (persistencia).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class PractitionerPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     * Mapea la relación N-a-N con supervisores, evitando ciclos infinitos.
     */
    public static Practitioner toDomain(PractitionerEntity entity) {
        if (entity == null) {
            return null;
        }

        Practitioner practitioner = new Practitioner();
        practitioner.setId(entity.getId());
        practitioner.setUser(UserPersistenceMapper.toDomain(entity.getUser()));
        practitioner.setStudentId(entity.getStudentId());
        practitioner.setStudyYear(entity.getStudyYear());

        // Nota: No mapeamos la relación inversa supervisors aquí para evitar ciclos
        // La relación se carga cuando sea necesario a través de consultas específicas

        return practitioner;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     * Mapea la relación N-a-N con supervisores, evitando ciclos infinitos.
     */
    public static PractitionerEntity toEntity(Practitioner practitioner) {
        if (practitioner == null) {
            return null;
        }

        PractitionerEntity entity = new PractitionerEntity();
        entity.setId(practitioner.getId());
        entity.setUser(UserPersistenceMapper.toEntity(practitioner.getUser()));
        entity.setStudentId(practitioner.getStudentId());
        entity.setStudyYear(practitioner.getStudyYear());

        // Nota: No mapeamos la relación inversa supervisors aquí para evitar ciclos
        // JPA manejará la relación bidireccional automáticamente

        return entity;
    }
}
