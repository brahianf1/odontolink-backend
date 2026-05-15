package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import site.utnpf.odontolink.domain.model.Role;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidad JPA para la tabla 'users'.
 * Representa la persistencia de un User del dominio.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(length = 20)
    private String phone;

    @Column
    private LocalDate birthDate;

    /**
     * Dirección postal del usuario (RF06). Es opcional y se modela como
     * string libre porque hoy no la usamos para georreferenciación: 255
     * caracteres son suficientes para una dirección humanamente legible.
     */
    @Column(length = 255)
    private String address;

    /**
     * URL pública (o pre-firmada) de la foto de perfil (RF06). En este PR no
     * gestionamos el archivo binario; la columna almacena exclusivamente la
     * URL ya resuelta por el frontend o por un futuro adapter de uploads.
     */
    @Column(name = "profile_picture_url", length = 512)
    private String profilePictureUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Marca temporal del ultimo evento que invalida sesiones JWT previas. Se
     * compara contra el claim {@code iat} de cada token recibido. Nullable
     * porque las cuentas creadas antes de introducir esta columna no tienen
     * el dato y el filtro JWT trata ese caso como "no rechazar".
     */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (!isActive) {
            isActive = true;
        }
    }

    // Constructores
    public UserEntity() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }
}
