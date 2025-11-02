package site.utnpf.odontolink.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el "Caso Clínico" completo.
 * Agrupa toda la información de un tratamiento que un practicante
 * le realiza a un paciente, desde el primer turno hasta el feedback.
 */
public class Attention {
    private Long id;

    /** Relación N-a-1: El paciente que recibe la atención */
    private Patient patient;

    /** Relación N-a-1: El practicante que realiza la atención */
    private Practitioner practitioner;

    /** Relación N-a-1: El tratamiento general que se está realizando */
    private Treatment treatment;

    private AttentionStatus status;
    private LocalDate startDate;

    /** Relación 1-a-N: La lista de todas las citas (turnos) asociadas a este caso */
    private List<Appointment> appointments;

    /** Relación 1-a-N: El historial de "evoluciones" de este caso */
    private List<ProgressNote> progressNotes;

    /** Relación 1-a-N: El feedback (de ambas partes) asociado a este caso */
    private List<Feedback> feedbackList;

    // Constructor sin argumentos (requerido por mappers de persistencia)
    public Attention() {
        this.appointments = new ArrayList<>();
        this.progressNotes = new ArrayList<>();
        this.feedbackList = new ArrayList<>();
    }

    public Attention(Patient patient, Practitioner practitioner, Treatment treatment) {
        this.patient = patient;
        this.practitioner = practitioner;
        this.treatment = treatment;
        this.status = AttentionStatus.IN_PROGRESS;
        this.startDate = LocalDate.now();
        this.appointments = new ArrayList<>();
        this.progressNotes = new ArrayList<>();
        this.feedbackList = new ArrayList<>();
    }

    // Comportamientos del Dominio Rico

    /**
     * Lógica de negocio para completar el caso.
     */
    public void complete() {
        if (this.status != AttentionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Solo se puede completar una atención que esté 'En Progreso'.");
        }
        // (Aquí podría ir lógica más compleja, como verificar si hay turnos pendientes)
        this.status = AttentionStatus.COMPLETED;
    }

    /**
     * Añade una nota de progreso (evolución) a este caso clínico.
     * Este método implementa la lógica del RF11 - CU 4.2: Registrar Evolución.
     *
     * Reglas de negocio:
     * - Solo se pueden añadir notas si el caso está en estado IN_PROGRESS
     * - El autor debe ser el practicante responsable o un supervisor
     * - Cada nota queda registrada con timestamp automático
     *
     * @param noteContent Contenido de la nota de evolución
     * @param author Usuario que registra la evolución (Practicante o Supervisor)
     * @throws IllegalStateException si el caso no está en progreso
     * @throws IllegalArgumentException si el autor no es el practicante o supervisor del caso
     */
    public void addProgressNote(String noteContent, User author) {
        if (this.status != AttentionStatus.IN_PROGRESS) {
            throw new IllegalStateException("No se pueden añadir notas a un caso que no esté 'En Progreso'.");
        }

        // Validar que el autor sea el practicante responsable del caso
        // Comparamos el User del Practitioner con el author
        boolean isAuthorized = (this.practitioner != null
                && this.practitioner.getUser() != null
                && this.practitioner.getUser().getId().equals(author.getId()));

        // Si el autor no es el practicante, verificar si es un supervisor mediante el Role
        if (!isAuthorized && author.getRole() == Role.ROLE_SUPERVISOR) {
            isAuthorized = true; // Los supervisores pueden agregar notas a cualquier caso
        }

        if (!isAuthorized) {
            throw new IllegalArgumentException("Solo el practicante responsable o un supervisor pueden añadir notas de progreso.");
        }

        ProgressNote newNote = new ProgressNote(this, noteContent, author);
        this.progressNotes.add(newNote);
    }

    /**
     * Lógica de negocio para agendar un nuevo turno para ESTE caso.
     */
    public Appointment scheduleAppointment(LocalDateTime time, String motive, int durationInMinutes) {
        if (this.status != AttentionStatus.IN_PROGRESS) {
            throw new IllegalStateException("No se pueden agendar turnos para un caso cerrado o cancelado.");
        }
        Appointment newAppointment = new Appointment(this, time, motive, durationInMinutes);
        this.appointments.add(newAppointment);
        return newAppointment;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public void setTreatment(Treatment treatment) {
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

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    public List<ProgressNote> getProgressNotes() {
        return progressNotes;
    }

    public void setProgressNotes(List<ProgressNote> progressNotes) {
        this.progressNotes = progressNotes;
    }

    public List<Feedback> getFeedbackList() {
        return feedbackList;
    }

    public void setFeedbackList(List<Feedback> feedbackList) {
        this.feedbackList = feedbackList;
    }
}