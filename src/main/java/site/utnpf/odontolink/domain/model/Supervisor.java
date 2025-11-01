package site.utnpf.odontolink.domain.model;

import java.util.Set;

/**
 * Contiene solo los datos ESPECÍFICOS del docente/supervisor.
 * Se vincula 1-a-1 con la entidad User.
 */
public class Supervisor {
    private Long id;

    /** Relación 1-a-1: Un Supervisor ES un User */
    private User user;

    // Campos Específicos del Supervisor
    private String specialty; // "Especialidad"
    private String employeeId; // "Legajo Docente"

    /**
     * Relación N-a-N: Un supervisor gestiona múltiples practicantes a su cargo.
     */
    private Set<Practitioner> supervisedPractitioners;

    // Constructores
    public Supervisor() {
    }

    public Supervisor(User user, String specialty, String employeeId) {
        this.user = user;
        this.specialty = specialty;
        this.employeeId = employeeId;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Set<Practitioner> getSupervisedPractitioners() {
        return supervisedPractitioners;
    }

    public void setSupervisedPractitioners(Set<Practitioner> supervisedPractitioners) {
        this.supervisedPractitioners = supervisedPractitioners;
    }
}