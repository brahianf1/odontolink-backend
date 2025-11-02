package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AppointmentEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * Verifica si existe una colisión de horario para un practicante en un rango de tiempo.
     * Este método es fundamental para el inventario dinámico, ya que verifica si un rango
     * [startTime, endTime) se solapa con algún turno existente del practicante.
     *
     * Lógica de solapamiento: Dos rangos [A1, A2) y [B1, B2) se solapan si A1 < B2 AND B1 < A2
     *
     * @param practitionerId ID del practicante
     * @param startTime Inicio del rango a verificar
     * @param endTime Fin del rango a verificar
     * @param excludeStatus Estado de turnos a excluir (típicamente CANCELLED)
     * @return true si existe al menos un turno que se solapa con el rango especificado
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AppointmentEntity a " +
           "WHERE a.attention.practitioner.id = :practitionerId " +
           "AND a.status <> :excludeStatus " +
           "AND a.appointmentTime < :endTime " +
           "AND FUNCTION('TIMESTAMPADD', MINUTE, a.durationInMinutes, a.appointmentTime) > :startTime")
    boolean hasCollisionInTimeRange(
            @Param("practitionerId") Long practitionerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeStatus") AppointmentStatus excludeStatus
    );

    /**
     * Verifica si existen turnos agendados (SCHEDULED) para una atención específica
     * que ocurran en el futuro (después de la fecha/hora especificada).
     *
     * Esta consulta es fundamental para la regla de negocio de finalización de casos:
     * "No se puede cerrar un caso si tiene turnos futuros agendados".
     *
     * @param attentionId ID de la atención (caso clínico)
     * @param status Estado del turno a buscar (típicamente SCHEDULED)
     * @param dateTime Fecha/hora de referencia (típicamente LocalDateTime.now())
     * @return true si existen turnos futuros agendados, false en caso contrario
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AppointmentEntity a " +
           "WHERE a.attention.id = :attentionId " +
           "AND a.status = :status " +
           "AND a.appointmentTime >= :dateTime")
    boolean existsByAttentionIdAndStatusAndAppointmentTimeGreaterThanEqual(
            @Param("attentionId") Long attentionId,
            @Param("status") AppointmentStatus status,
            @Param("dateTime") LocalDateTime dateTime
    );

    /**
     * Verifica si existen turnos en estado SCHEDULED para una atención específica
     * que ya ocurrieron en el pasado (antes de la fecha/hora especificada).
     *
     * Esta consulta es útil para detectar turnos que no fueron marcados como
     * COMPLETED o NO_SHOW por el practicante.
     *
     * @param attentionId ID de la atención (caso clínico)
     * @param status Estado del turno a buscar (típicamente SCHEDULED)
     * @param dateTime Fecha/hora de referencia (típicamente LocalDateTime.now())
     * @return true si existen turnos pasados sin marcar, false en caso contrario
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AppointmentEntity a " +
           "WHERE a.attention.id = :attentionId " +
           "AND a.status = :status " +
           "AND a.appointmentTime < :dateTime")
    boolean existsByAttentionIdAndStatusAndAppointmentTimeLessThan(
            @Param("attentionId") Long attentionId,
            @Param("status") AppointmentStatus status,
            @Param("dateTime") LocalDateTime dateTime
    );

    /**
     * Busca un turno por ID cargando su Attention y Practitioner asociado.
     * Este método es necesario para operaciones que requieren validar
     * la propiedad del turno (ej: marcar como completado/no-show).
     *
     * Carga eager:
     * - Attention
     * - Practitioner con su User
     *
     * @param id ID del turno
     * @return Optional conteniendo el turno con sus relaciones cargadas
     */
    @Query("SELECT a FROM AppointmentEntity a " +
           "JOIN FETCH a.attention att " +
           "JOIN FETCH att.practitioner pr " +
           "JOIN FETCH pr.user " +
           "WHERE a.id = :id")
    Optional<AppointmentEntity> findByIdWithAttention(@Param("id") Long id);

    /**
     * Actualiza únicamente el estado de un turno sin modificar otras relaciones.
     * Este método es más eficiente y seguro que cargar, mapear y volver a persistir toda la entidad,
     * ya que evita problemas con referencias bidireccionales y optimiza la transacción.
     *
     * Se utiliza en operaciones como marcar turno como completado o ausente,
     * donde solo necesitamos cambiar el estado sin tocar la estructura de relaciones.
     *
     * @param id ID del turno a actualizar
     * @param status Nuevo estado del turno
     * @return Número de registros actualizados (1 si existía, 0 si no)
     */
    @Modifying
    @Query("UPDATE AppointmentEntity a SET a.status = :status WHERE a.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") AppointmentStatus status);
}
