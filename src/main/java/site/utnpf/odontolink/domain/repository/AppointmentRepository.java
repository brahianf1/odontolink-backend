package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Appointment (Turno).
 * Puerto de salida (Output Port) en arquitectura hexagonal.
 */
public interface AppointmentRepository {

    /**
     * Guarda un nuevo turno o actualiza uno existente.
     */
    Appointment save(Appointment appointment);

    /**
     * Busca un turno por su ID.
     */
    Optional<Appointment> findById(Long id);

    /**
     * Busca un turno por su ID cargando su Attention y Practitioner asociado.
     * Este método es necesario para operaciones que requieren validar
     * la propiedad del turno (ej: marcar como completado/no-show).
     *
     * @param id ID del turno
     * @return Optional conteniendo el turno con sus relaciones cargadas
     */
    Optional<Appointment> findByIdWithAttention(Long id);

    /**
     * Obtiene todos los turnos de un paciente específico.
     */
    List<Appointment> findByPatient(Patient patient);

    /**
     * Obtiene todos los turnos de un paciente por su ID.
     */
    List<Appointment> findByPatientId(Long patientId);

    /**
     * Obtiene todos los turnos de un practicante específico.
     */
    List<Appointment> findByPractitioner(Practitioner practitioner);

    /**
     * Obtiene todos los turnos de un practicante por su ID.
     */
    List<Appointment> findByPractitionerId(Long practitionerId);

    /**
     * Obtiene los turnos de un paciente filtrados por estado.
     */
    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    /**
     * Obtiene los turnos de un practicante filtrados por estado.
     */
    List<Appointment> findByPractitionerIdAndStatus(Long practitionerId, AppointmentStatus status);

    /**
     * Verifica si existe un conflicto de horario para un paciente en una fecha/hora específica.
     * Útil para evitar que un paciente tenga dos turnos al mismo tiempo.
     */
    boolean existsByPatientAndAppointmentTimeAndStatusNot(
            Patient patient,
            LocalDateTime appointmentTime,
            AppointmentStatus status
    );

    /**
     * Verifica si existe un conflicto de horario para un paciente por ID.
     */
    boolean existsByPatientIdAndAppointmentTimeAndStatusNot(
            Long patientId,
            LocalDateTime appointmentTime,
            AppointmentStatus status
    );

    /**
     * Verifica si existe un conflicto de horario para un practicante en una fecha/hora específica.
     */
    boolean existsByPractitionerAndAppointmentTimeAndStatusNot(
            Practitioner practitioner,
            LocalDateTime appointmentTime,
            AppointmentStatus status
    );

    /**
     * Verifica si existe un conflicto de horario para un practicante por ID.
     */
    boolean existsByPractitionerIdAndAppointmentTimeAndStatusNot(
            Long practitionerId,
            LocalDateTime appointmentTime,
            AppointmentStatus status
    );

    /**
     * Verifica si existe un conflicto de horario para un practicante en un rango de tiempo.
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
    boolean hasCollisionInTimeRange(
            Long practitionerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AppointmentStatus excludeStatus
    );

    /**
     * Obtiene todos los turnos de un practicante para una fecha específica.
     * Útil para calcular el inventario de slots disponibles.
     *
     * @param practitionerId ID del practicante
     * @param date Fecha para la cual buscar turnos
     * @return Lista de turnos del practicante en esa fecha
     */
    List<Appointment> findByPractitionerIdAndDate(Long practitionerId, LocalDateTime date);

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
    boolean existsByAttentionIdAndStatusAndAppointmentTimeGreaterThanEqual(
            Long attentionId,
            AppointmentStatus status,
            LocalDateTime dateTime
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
    boolean existsByAttentionIdAndStatusAndAppointmentTimeLessThan(
            Long attentionId,
            AppointmentStatus status,
            LocalDateTime dateTime
    );

    /**
     * Actualiza únicamente el estado de un turno sin cargar ni modificar otras relaciones.
     * Este método es más eficiente y seguro que el método save() tradicional para operaciones
     * que solo requieren cambiar el estado, ya que:
     * 1. Evita el problema de referencias bidireccionales nulas en el mapeo
     * 2. Ejecuta una consulta UPDATE directa en la base de datos
     * 3. No requiere cargar la entidad completa en memoria
     * 4. Es más eficiente en términos de rendimiento
     *
     * Se utiliza en operaciones como marcar un turno como completado o ausente (no-show),
     * donde el único cambio necesario es actualizar el campo de estado.
     *
     * @param appointmentId ID del turno a actualizar
     * @param newStatus Nuevo estado a asignar al turno
     * @return true si se actualizó correctamente (el turno existía), false si no existe
     */
    boolean updateStatus(Long appointmentId, AppointmentStatus newStatus);
}
