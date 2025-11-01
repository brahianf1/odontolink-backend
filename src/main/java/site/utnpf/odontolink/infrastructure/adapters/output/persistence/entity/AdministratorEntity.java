package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA para la tabla 'administrators'.
 * Representa la persistencia de un Administrator del dominio.
 */
@Entity
@Table(name = "administrators")
public class AdministratorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    // Campos adicionales específicos del administrador pueden agregarse aquí
    // Por ejemplo: nivel de permisos, área de responsabilidad, etc.

    // Constructores
    public AdministratorEntity() {
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
}
