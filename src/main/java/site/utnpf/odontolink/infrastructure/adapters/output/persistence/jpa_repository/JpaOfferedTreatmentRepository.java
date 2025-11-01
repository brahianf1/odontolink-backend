package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.OfferedTreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para OfferedTreatmentEntity.
 * Proporciona operaciones CRUD y consultas personalizadas para el catálogo personal del practicante.
 */
public interface JpaOfferedTreatmentRepository extends JpaRepository<OfferedTreatmentEntity, Long> {

    /**
     * Obtiene todas las ofertas de tratamientos de un practicante.
     */
    List<OfferedTreatmentEntity> findByPractitioner(PractitionerEntity practitioner);

    /**
     * Obtiene todas las ofertas de tratamientos de un practicante por su ID.
     */
    List<OfferedTreatmentEntity> findByPractitionerId(Long practitionerId);

    /**
     * Obtiene todas las ofertas de tratamientos filtradas por tipo de tratamiento.
     */
    List<OfferedTreatmentEntity> findByTreatmentId(Long treatmentId);

    /**
     * Verifica si un practicante ya ofrece un tratamiento específico.
     */
    boolean existsByPractitionerAndTreatment(PractitionerEntity practitioner, TreatmentEntity treatment);

    /**
     * Busca una oferta específica por practicante y tratamiento.
     */
    Optional<OfferedTreatmentEntity> findByPractitionerAndTreatment(PractitionerEntity practitioner, TreatmentEntity treatment);

    /**
     * Verifica si existen turnos activos asociados a una oferta de tratamiento.
     * Los estados activos son: SCHEDULED, CONFIRMED, RESCHEDULED
     *
     * TODO: Esta consulta será implementada cuando se cree la entidad AppointmentEntity.
     * Por ahora, se usa un método por defecto que retorna false (no hay turnos activos).
     * Esto permite que la funcionalidad de eliminación del catálogo funcione correctamente
     * hasta que se implemente completamente el módulo de Appointments.
     *
     * Implementación futura (cuando exista AppointmentEntity):
     * @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
     *        "FROM AppointmentEntity a " +
     *        "WHERE a.offeredTreatment.id = :offeredTreatmentId " +
     *        "AND a.status IN ('SCHEDULED', 'CONFIRMED', 'RESCHEDULED')")
     */
    default boolean hasActiveAppointments(@Param("offeredTreatmentId") Long offeredTreatmentId) {
        // Implementación temporal: retorna false (no hay turnos activos)
        // Esto permite eliminar tratamientos del catálogo hasta que se implemente
        // completamente la funcionalidad de Appointments
        return false;
    }
}
