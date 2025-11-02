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
}
