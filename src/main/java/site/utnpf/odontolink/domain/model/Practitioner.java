package site.utnpf.odontolink.domain.model;

import java.util.Set;

/**
 * Contiene solo los datos ESPECÍFICOS del practicante.
 * Se vincula 1-a-1 con la entidad User.
 */
public class Practitioner {
    private Long id;

    /** Relación 1-a-1: Un Practicante ES un User */
    private User user;

    // Campos Específicos del Practicante
    private String studentId;     // "Legajo"
    private Integer studyYear;    // "Año Cursado"

    /**
     * Relación N-a-N: Un practicante puede ser supervisado
     * por múltiples docentes (ej. en diferentes cátedras).
     */
    private Set<Supervisor> supervisors;

    /**
     * Relación 1-a-N: El "Catálogo Personal" del practicante.
     * Son los tratamientos que él ha configurado y ofrece.
     */
    private Set<OfferedTreatment> offeredTreatments;

    /**
     * Relación 1-a-N: Un practicante gestiona múltiples casos de atención.
     * Representa "Mis Atenciones".
     */
    private Set<Attention> attentions;

    // Constructores
    public Practitioner() {
    }

    public Practitioner(User user, String studentId, Integer studyYear) {
        this.user = user;
        this.studentId = studentId;
        this.studyYear = studyYear;
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getStudyYear() {
        return studyYear;
    }

    public void setStudyYear(Integer studyYear) {
        this.studyYear = studyYear;
    }

    public Set<Supervisor> getSupervisors() {
        return supervisors;
    }

    public void setSupervisors(Set<Supervisor> supervisors) {
        this.supervisors = supervisors;
    }

    public Set<OfferedTreatment> getOfferedTreatments() {
        return offeredTreatments;
    }

    public void setOfferedTreatments(Set<OfferedTreatment> offeredTreatments) {
        this.offeredTreatments = offeredTreatments;
    }

    public Set<Attention> getAttentions() {
        return attentions;
    }

    public void setAttentions(Set<Attention> attentions) {
        this.attentions = attentions;
    }
}