package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para la tabla 'practitioners'.
 * Representa la persistencia de un Practitioner del dominio.
 */
@Entity
@Table(name = "practitioners")
public class PractitionerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(nullable = false, unique = true, length = 50)
    private String studentId;

    @Column(nullable = false)
    private Integer studyYear;

    /**
     * Relación N-a-N: Un practicante puede ser supervisado por múltiples supervisores.
     * Esta es la entidad "no dueña" de la relación (referencia a la tabla de unión).
     */
    @ManyToMany(mappedBy = "supervisedPractitioners", fetch = FetchType.LAZY)
    private Set<SupervisorEntity> supervisors = new HashSet<>();

    // Constructores
    public PractitionerEntity() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
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

    public Set<SupervisorEntity> getSupervisors() {
        return supervisors;
    }

    public void setSupervisors(Set<SupervisorEntity> supervisors) {
        this.supervisors = supervisors;
    }
}
