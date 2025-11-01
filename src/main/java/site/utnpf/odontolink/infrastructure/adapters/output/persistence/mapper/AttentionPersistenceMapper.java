package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AttentionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AppointmentEntity;

import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Attention (dominio) y AttentionEntity (persistencia).
 */
public class AttentionPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     * Mapea también la lista de Appointments hijos.
     */
    public static Attention toDomain(AttentionEntity entity) {
        if (entity == null) {
            return null;
        }

        Attention attention = new Attention();
        attention.setId(entity.getId());
        attention.setPatient(PatientPersistenceMapper.toDomain(entity.getPatient()));
        attention.setPractitioner(PractitionerPersistenceMapper.toDomain(entity.getPractitioner()));
        attention.setTreatment(TreatmentPersistenceMapper.toDomain(entity.getTreatment()));
        attention.setStatus(entity.getStatus());
        attention.setStartDate(entity.getStartDate());

        // Mapear la lista de Appointments
        if (entity.getAppointments() != null) {
            attention.setAppointments(
                    entity.getAppointments().stream()
                            .map(appointmentEntity -> {
                                Appointment appointment = AppointmentPersistenceMapper.toDomain(appointmentEntity);
                                // Establecer la relación bidireccional
                                appointment.setAttention(attention);
                                return appointment;
                            })
                            .collect(Collectors.toList())
            );
        }

        return attention;
    }

    /**
     * Convierte de entidad JPA a modelo de dominio SIN mapear la lista de Appointments.
     * Usado para evitar ciclos infinitos cuando se mapea desde Appointment hacia Attention.
     *
     * Este método mapea:
     * - Patient con su User
     * - Practitioner con su User
     * - Treatment
     * - Datos propios de Attention
     *
     * NO mapea:
     * - Lista de Appointments (para evitar ciclos)
     */
    public static Attention toDomainShallow(AttentionEntity entity) {
        if (entity == null) {
            return null;
        }

        Attention attention = new Attention();
        attention.setId(entity.getId());
        attention.setPatient(PatientPersistenceMapper.toDomain(entity.getPatient()));
        attention.setPractitioner(PractitionerPersistenceMapper.toDomain(entity.getPractitioner()));
        attention.setTreatment(TreatmentPersistenceMapper.toDomain(entity.getTreatment()));
        attention.setStatus(entity.getStatus());
        attention.setStartDate(entity.getStartDate());

        // NO mapeamos la lista de Appointments para evitar ciclos infinitos

        return attention;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     * Mapea también la lista de Appointments hijos y establece las relaciones bidireccionales.
     */
    public static AttentionEntity toEntity(Attention attention) {
        if (attention == null) {
            return null;
        }

        AttentionEntity entity = new AttentionEntity();
        entity.setId(attention.getId());
        entity.setPatient(PatientPersistenceMapper.toEntity(attention.getPatient()));
        entity.setPractitioner(PractitionerPersistenceMapper.toEntity(attention.getPractitioner()));
        entity.setTreatment(TreatmentPersistenceMapper.toEntity(attention.getTreatment()));
        entity.setStatus(attention.getStatus());
        entity.setStartDate(attention.getStartDate());

        // Mapear la lista de Appointments y establecer la relación bidireccional
        if (attention.getAppointments() != null) {
            entity.setAppointments(
                    attention.getAppointments().stream()
                            .map(appointment -> {
                                AppointmentEntity appointmentEntity = AppointmentPersistenceMapper.toEntity(appointment);
                                // Establecer la relación bidireccional
                                appointmentEntity.setAttention(entity);
                                return appointmentEntity;
                            })
                            .collect(Collectors.toList())
            );
        }

        return entity;
    }
}
