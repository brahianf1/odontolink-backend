package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AppointmentEntity;

/**
 * Mapper para convertir entre Appointment (dominio) y AppointmentEntity (persistencia).
 */
public class AppointmentPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     * IMPORTANTE: No mapea 'attention' para evitar ciclos infinitos.
     * El Attention ya contiene la lista de Appointments.
     */
    public static Appointment toDomain(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }

        Appointment appointment = new Appointment();
        appointment.setId(entity.getId());
        appointment.setAppointmentTime(entity.getAppointmentTime());
        appointment.setMotive(entity.getMotive());
        appointment.setStatus(entity.getStatus());
        // NOTE: No mapeamos 'attention' aquí para evitar ciclos

        return appointment;
    }

    /**
     * Convierte de entidad JPA a modelo de dominio CON relaciones cargadas.
     * Usado cuando las relaciones ya fueron cargadas mediante JOIN FETCH.
     *
     * IMPORTANTE: Este método asume que entity.getAttention() y todas sus relaciones
     * ya fueron cargadas (EAGER) mediante JOIN FETCH en la query.
     * No intenta mapear recursivamente para evitar ciclos infinitos.
     *
     * Solo establece la referencia a Attention en el Appointment para que
     * el AppointmentRestMapper pueda acceder a los datos relacionados.
     */
    public static Appointment toDomainWithRelations(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }

        Appointment appointment = new Appointment();
        appointment.setId(entity.getId());
        appointment.setAppointmentTime(entity.getAppointmentTime());
        appointment.setMotive(entity.getMotive());
        appointment.setStatus(entity.getStatus());

        // Mapear la Attention con sus relaciones ya cargadas
        if (entity.getAttention() != null) {
            appointment.setAttention(AttentionPersistenceMapper.toDomainShallow(entity.getAttention()));
        }

        return appointment;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     * IMPORTANTE: No mapea 'attention' para evitar ciclos infinitos.
     * La relación se establece en AttentionPersistenceMapper.
     */
    public static AppointmentEntity toEntity(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        AppointmentEntity entity = new AppointmentEntity();
        entity.setId(appointment.getId());
        entity.setAppointmentTime(appointment.getAppointmentTime());
        entity.setMotive(appointment.getMotive());
        entity.setStatus(appointment.getStatus());
        // NOTE: No mapeamos 'attention' aquí para evitar ciclos

        return entity;
    }
}
