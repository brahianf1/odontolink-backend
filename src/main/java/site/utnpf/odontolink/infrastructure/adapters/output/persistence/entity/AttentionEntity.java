package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import site.utnpf.odontolink.domain.model.AttentionStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla 'attentions'.
 * Representa un "Caso Clínico" completo en la base de datos.
 *
 * Esta entidad es el agregado raíz de la relación con Appointment.
 * Al guardar una AttentionEntity, se guardan automáticamente sus AppointmentEntity hijos
 * gracias a CascadeType.ALL.
 */
@Entity
@Table(name = "attentions")
public class AttentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerEntity practitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_id", nullable = false)
    private TreatmentEntity treatment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttentionStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * Relación OneToMany con AppointmentEntity (bidireccional).
     *
     * CascadeType.ALL: Todas las operaciones (persist, merge, remove, etc.) se propagan a los Appointments.
     * orphanRemoval = true: Si un Appointment se remueve de la lista, se elimina de la BD.
     *
     * mappedBy = "attention": Indica que AppointmentEntity tiene el lado owner de la relación.
     */
    @OneToMany(mappedBy = "attention", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentEntity> appointments = new ArrayList<>();

    /**
     * Relación OneToMany con ProgressNoteEntity (bidireccional).
     *
     * CascadeType.ALL: Todas las operaciones (persist, merge, remove, etc.) se propagan a las ProgressNotes.
     * orphanRemoval = true: Si una ProgressNote se remueve de la lista, se elimina de la BD.
     *
     * mappedBy = "attention": Indica que ProgressNoteEntity tiene el lado owner de la relación.
     */
    @OneToMany(mappedBy = "attention", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgressNoteEntity> progressNotes = new ArrayList<>();

    // Constructores
    public AttentionEntity() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public PractitionerEntity getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(PractitionerEntity practitioner) {
        this.practitioner = practitioner;
    }

    public TreatmentEntity getTreatment() {
        return treatment;
    }

    public void setTreatment(TreatmentEntity treatment) {
        this.treatment = treatment;
    }

    public AttentionStatus getStatus() {
        return status;
    }

    public void setStatus(AttentionStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public List<AppointmentEntity> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<AppointmentEntity> appointments) {
        this.appointments = appointments;
    }

    /**
     * Método de utilidad para mantener la consistencia bidireccional.
     * Agrega un AppointmentEntity a la lista y establece la relación inversa.
     */
    public void addAppointment(AppointmentEntity appointment) {
        this.appointments.add(appointment);
        appointment.setAttention(this);
    }

    /**
     * Método de utilidad para mantener la consistencia bidireccional.
     * Remueve un AppointmentEntity de la lista y rompe la relación inversa.
     */
    public void removeAppointment(AppointmentEntity appointment) {
        this.appointments.remove(appointment);
        appointment.setAttention(null);
    }

    public List<ProgressNoteEntity> getProgressNotes() {
        return progressNotes;
    }

    public void setProgressNotes(List<ProgressNoteEntity> progressNotes) {
        this.progressNotes = progressNotes;
    }

    /**
     * Método de utilidad para mantener la consistencia bidireccional.
     * Agrega una ProgressNoteEntity a la lista y establece la relación inversa.
     */
    public void addProgressNote(ProgressNoteEntity progressNote) {
        this.progressNotes.add(progressNote);
        progressNote.setAttention(this);
    }

    /**
     * Método de utilidad para mantener la consistencia bidireccional.
     * Remueve una ProgressNoteEntity de la lista y rompe la relación inversa.
     */
    public void removeProgressNote(ProgressNoteEntity progressNote) {
        this.progressNotes.remove(progressNote);
        progressNote.setAttention(null);
    }
}
