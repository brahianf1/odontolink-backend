package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.domain.model.AttentionStatus;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AttentionEntity;

import java.util.List;
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
}
