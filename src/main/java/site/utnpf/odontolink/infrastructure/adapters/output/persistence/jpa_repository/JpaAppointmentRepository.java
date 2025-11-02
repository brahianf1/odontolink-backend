package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AppointmentEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    List<AppointmentEntity> findByAttention_Patient_Id(Long patientId);

    List<AppointmentEntity> findByAttention_Practitioner_Id(Long practitionerId);

    List<AppointmentEntity> findByAttention_Patient_IdAndStatus(Long patientId, AppointmentStatus status);

    List<AppointmentEntity> findByAttention_Practitioner_IdAndStatus(Long practitionerId, AppointmentStatus status);

    /**
     * Busca turnos de un paciente con estado específico, cargando todas las relaciones necesarias
     * mediante JOIN FETCH para evitar el problema N+1 y lazy loading.
     *
     * Carga eager:
     * - Attention
     * - Patient con su User
     * - Practitioner con su User
     * - Treatment
     */
    @Query("SELECT a FROM AppointmentEntity a " +
           "JOIN FETCH a.attention att " +
           "JOIN FETCH att.patient p " +
           "JOIN FETCH p.user " +
           "JOIN FETCH att.practitioner pr " +
           "JOIN FETCH pr.user " +
           "JOIN FETCH att.treatment " +
           "WHERE p.id = :patientId " +
           "AND a.status = :status")
    List<AppointmentEntity> findByPatientIdAndStatusWithDetails(
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status
    );

    /**
     * Busca turnos de un practicante con estado específico, cargando todas las relaciones necesarias
     * mediante JOIN FETCH para evitar el problema N+1 y lazy loading.
     *
     * Carga eager:
     * - Attention
     * - Patient con su User
     * - Practitioner con su User
     * - Treatment
     */
    @Query("SELECT a FROM AppointmentEntity a " +
           "JOIN FETCH a.attention att " +
           "JOIN FETCH att.patient p " +
           "JOIN FETCH p.user " +
           "JOIN FETCH att.practitioner pr " +
           "JOIN FETCH pr.user " +
           "JOIN FETCH att.treatment " +
           "WHERE pr.id = :practitionerId " +
           "AND a.status = :status")
    List<AppointmentEntity> findByPractitionerIdAndStatusWithDetails(
            @Param("practitionerId") Long practitionerId,
            @Param("status") AppointmentStatus status
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AppointmentEntity a " +
           "WHERE a.attention.patient.id = :patientId " +
           "AND a.appointmentTime = :appointmentTime " +
           "AND a.status <> :excludedStatus")
    boolean existsByPatientIdAndAppointmentTimeAndStatusNot(
            @Param("patientId") Long patientId,
            @Param("appointmentTime") LocalDateTime appointmentTime,
            @Param("excludedStatus") AppointmentStatus excludedStatus
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AppointmentEntity a " +
           "WHERE a.attention.practitioner.id = :practitionerId " +
           "AND a.appointmentTime = :appointmentTime " +
           "AND a.status <> :excludedStatus")
    boolean existsByPractitionerIdAndAppointmentTimeAndStatusNot(
            @Param("practitionerId") Long practitionerId,
            @Param("appointmentTime") LocalDateTime appointmentTime,
            @Param("excludedStatus") AppointmentStatus excludedStatus
    );

    /**
     * Busca turnos de un practicante para una fecha específica.
     * Este método es crucial para el cálculo del inventario dinámico.
     *
     * @param practitionerId ID del practicante
     * @param startOfDay Inicio del día (00:00:00)
     * @param endOfDay Fin del día (23:59:59)
     * @return Lista de turnos en ese rango
     */
    @Query("SELECT a FROM AppointmentEntity a " +
           "WHERE a.attention.practitioner.id = :practitionerId " +
           "AND a.appointmentTime >= :startOfDay " +
           "AND a.appointmentTime < :endOfDay")
    List<AppointmentEntity> findByPractitionerIdAndDateRange(
            @Param("practitionerId") Long practitionerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
