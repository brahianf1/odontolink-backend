package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import site.utnpf.odontolink.domain.model.AppointmentStatus;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla 'appointments'.
 * Representa un "Turno" o "Cita" en la base de datos.
 *
 * Esta entidad PERTENECE a una AttentionEntity (relación ManyToOne).
 * No tiene sentido sin su Attention padre.
 */
@Entity
@Table(name = "appointments")
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación ManyToOne con AttentionEntity (bidireccional).
     *
     * JoinColumn indica que esta tabla tiene la FK hacia 'attentions'.
     * Este es el lado "owner" de la relación bidireccional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attention_id", nullable = false)
    private AttentionEntity attention;

    @Column(nullable = false)
    private LocalDateTime appointmentTime;

    @Column(columnDefinition = "TEXT")
    private String motive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    // Constructores
    public AppointmentEntity() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttentionEntity getAttention() {
        return attention;
    }

    public void setAttention(AttentionEntity attention) {
        this.attention = attention;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getMotive() {
        return motive;
    }

    public void setMotive(String motive) {
        this.motive = motive;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
