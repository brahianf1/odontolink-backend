package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para la tabla 'supervisors'.
 * Representa la persistencia de un Supervisor del dominio.
 */
@Entity
@Table(name = "supervisors")
public class SupervisorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(nullable = false, unique = true, length = 50)
    private String employeeId;

    /**
     * Relación N-a-N: Un supervisor gestiona múltiples practicantes.
     * Esta es la entidad "dueña" de la relación (mappedBy en el otro lado).
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "supervisor_practitioner",
        joinColumns = @JoinColumn(name = "supervisor_id"),
        inverseJoinColumns = @JoinColumn(name = "practitioner_id")
    )
    private Set<PractitionerEntity> supervisedPractitioners = new HashSet<>();

    // Constructores
    public SupervisorEntity() {
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

    public Set<PractitionerEntity> getSupervisedPractitioners() {
        return supervisedPractitioners;
    }

    public void setSupervisedPractitioners(Set<PractitionerEntity> supervisedPractitioners) {
        this.supervisedPractitioners = supervisedPractitioners;
    }
}
