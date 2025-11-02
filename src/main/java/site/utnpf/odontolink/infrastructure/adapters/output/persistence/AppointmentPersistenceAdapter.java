package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AppointmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAppointmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AppointmentPersistenceMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Appointment (Hexagonal Architecture).
 * Implementa la interfaz del dominio AppointmentRepository usando JPA.
 * Puerto de salida (Output Adapter).
 */
@Component
public class AppointmentPersistenceAdapter implements AppointmentRepository {

    private final JpaAppointmentRepository jpaAppointmentRepository;

    public AppointmentPersistenceAdapter(JpaAppointmentRepository jpaAppointmentRepository) {
        this.jpaAppointmentRepository = jpaAppointmentRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = AppointmentPersistenceMapper.toEntity(appointment);
        AppointmentEntity savedEntity = jpaAppointmentRepository.save(entity);
        return AppointmentPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return jpaAppointmentRepository.findById(id)
                .map(AppointmentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Appointment> findByIdWithAttention(Long id) {
        return jpaAppointmentRepository.findByIdWithAttention(id)
                .map(AppointmentPersistenceMapper::toDomainWithRelations);
    }

    @Override
    public List<Appointment> findByPatient(Patient patient) {
        return findByPatientId(patient.getId());
    }

    @Override
    public List<Appointment> findByPatientId(Long patientId) {
        return jpaAppointmentRepository.findByAttention_Patient_Id(patientId).stream()
                .map(AppointmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByPractitioner(Practitioner practitioner) {
        return findByPractitionerId(practitioner.getId());
    }

    @Override
    public List<Appointment> findByPractitionerId(Long practitionerId) {
        return jpaAppointmentRepository.findByAttention_Practitioner_Id(practitionerId).stream()
                .map(AppointmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status) {
        return jpaAppointmentRepository.findByPatientIdAndStatusWithDetails(patientId, status).stream()
                .map(AppointmentPersistenceMapper::toDomainWithRelations)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByPractitionerIdAndStatus(Long practitionerId, AppointmentStatus status) {
        return jpaAppointmentRepository.findByPractitionerIdAndStatusWithDetails(practitionerId, status).stream()
                .map(AppointmentPersistenceMapper::toDomainWithRelations)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPatientAndAppointmentTimeAndStatusNot(
            Patient patient,
            LocalDateTime appointmentTime,
            AppointmentStatus status) {
        return existsByPatientIdAndAppointmentTimeAndStatusNot(patient.getId(), appointmentTime, status);
    }

    @Override
    public boolean existsByPatientIdAndAppointmentTimeAndStatusNot(
            Long patientId,
            LocalDateTime appointmentTime,
            AppointmentStatus status) {
        return jpaAppointmentRepository.existsByPatientIdAndAppointmentTimeAndStatusNot(
                patientId,
                appointmentTime,
                status
        );
    }

    @Override
    public boolean existsByPractitionerAndAppointmentTimeAndStatusNot(
            Practitioner practitioner,
            LocalDateTime appointmentTime,
            AppointmentStatus status) {
        return existsByPractitionerIdAndAppointmentTimeAndStatusNot(practitioner.getId(), appointmentTime, status);
    }

    @Override
    public boolean existsByPractitionerIdAndAppointmentTimeAndStatusNot(
            Long practitionerId,
            LocalDateTime appointmentTime,
            AppointmentStatus status) {
        return jpaAppointmentRepository.existsByPractitionerIdAndAppointmentTimeAndStatusNot(
                practitionerId,
                appointmentTime,
                status
        );
    }

    @Override
    public boolean hasCollisionInTimeRange(
            Long practitionerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AppointmentStatus excludeStatus) {

        // Obtener todos los turnos del practicante en ese rango de fechas
        LocalDateTime startOfDay = startTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = endTime.toLocalDate().plusDays(1).atStartOfDay();

        List<AppointmentEntity> appointments = jpaAppointmentRepository.findByPractitionerIdAndDateRange(
                practitionerId,
                startOfDay,
                endOfDay
        );

        // Filtrar los que no tienen el estado excluido y verificar solapamiento
        return appointments.stream()
                .filter(apt -> apt.getStatus() != excludeStatus)
                .anyMatch(apt -> {
                    LocalDateTime aptStart = apt.getAppointmentTime();
                    LocalDateTime aptEnd = aptStart.plusMinutes(apt.getDurationInMinutes());

                    // Detectar solapamiento: dos rangos se solapan si uno comienza antes de que el otro termine
                    return startTime.isBefore(aptEnd) && endTime.isAfter(aptStart);
                });
    }

    @Override
    public List<Appointment> findByPractitionerIdAndDate(Long practitionerId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().plusDays(1).atStartOfDay();

        return jpaAppointmentRepository.findByPractitionerIdAndDateRange(
                practitionerId,
                startOfDay,
                endOfDay
        ).stream()
                .map(AppointmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAttentionIdAndStatusAndAppointmentTimeGreaterThanEqual(
            Long attentionId,
            AppointmentStatus status,
            LocalDateTime dateTime) {
        return jpaAppointmentRepository.existsByAttentionIdAndStatusAndAppointmentTimeGreaterThanEqual(
                attentionId,
                status,
                dateTime
        );
    }

    @Override
    public boolean existsByAttentionIdAndStatusAndAppointmentTimeLessThan(
            Long attentionId,
            AppointmentStatus status,
            LocalDateTime dateTime) {
        return jpaAppointmentRepository.existsByAttentionIdAndStatusAndAppointmentTimeLessThan(
                attentionId,
                status,
                dateTime
        );
    }

    /**
     * Actualiza únicamente el estado de un turno.
     * Este método es más eficiente que save() para operaciones que solo modifican el estado,
     * ya que evita el mapeo completo de la entidad y sus relaciones bidireccionales.
     *
     * @param appointmentId ID del turno
     * @param newStatus Nuevo estado
     * @return true si se actualizó correctamente, false si el turno no existe
     */
    @Override
    public boolean updateStatus(Long appointmentId, AppointmentStatus newStatus) {
        int rowsAffected = jpaAppointmentRepository.updateStatus(appointmentId, newStatus);
        return rowsAffected > 0;
    }
}
