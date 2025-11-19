package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.domain.model.AttentionStatus;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AttentionEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JpaAttentionRepository extends JpaRepository<AttentionEntity, Long> {

    List<AttentionEntity> findByPatient_Id(Long patientId);

    List<AttentionEntity> findByPractitioner_Id(Long practitionerId);

    List<AttentionEntity> findByPatient_IdAndStatus(Long patientId, AttentionStatus status);

    List<AttentionEntity> findByPractitioner_IdAndStatus(Long practitionerId, AttentionStatus status);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AttentionEntity a " +
           "WHERE a.patient.id = :patientId " +
           "AND a.practitioner.id = :practitionerId " +
           "AND a.treatment.id = :treatmentId " +
           "AND a.status = :status")
    boolean existsByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
            @Param("patientId") Long patientId,
            @Param("practitionerId") Long practitionerId,
            @Param("treatmentId") Long treatmentId,
            @Param("status") AttentionStatus status
    );

    Optional<AttentionEntity> findByPatient_IdAndPractitioner_IdAndTreatment_IdAndStatus(
            Long patientId,
            Long practitionerId,
            Long treatmentId,
            AttentionStatus status
    );

    int countByPractitioner_IdAndTreatment_IdAndStatus(
            Long practitionerId,
            Long treatmentId,
            AttentionStatus status
    );

    /**
     * Consulta optimizada para calcular el progreso de todas las ofertas de un practicante
     * en una sola operación de base de datos (evita problema N+1).
     *
     * Esta consulta agrupa las atenciones completadas por tratamiento,
     * retornando un Map<TreatmentId, Count> que permite enriquecer el DTO de respuesta
     * con el progreso actual sin ejecutar múltiples consultas.
     *
     * @param practitionerId ID del practicante
     * @param status Estado de las atenciones a contar (típicamente COMPLETED)
     * @return Map donde la clave es el ID del tratamiento y el valor es el conteo
     */
    @Query("SELECT a.treatment.id AS treatmentId, COUNT(a) AS count " +
           "FROM AttentionEntity a " +
           "WHERE a.practitioner.id = :practitionerId " +
           "AND a.status = :status " +
           "GROUP BY a.treatment.id")
    List<Object[]> countByPractitionerGroupByTreatment(
            @Param("practitionerId") Long practitionerId,
            @Param("status") AttentionStatus status
    );
}
