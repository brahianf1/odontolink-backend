package site.utnpf.odontolink.domain.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Representa la entidad central de AUTENTICACIÓN y PERFIL.
 * Contiene todos los campos comunes a todas las personas.
 * Spring Security interactuará directamente con esta entidad.
 */
public class User {
    private Long id;
    private String email;
    private String password;
    private Role role;
    private boolean isActive;

    // Campos de Perfil Comunes
    private String firstName;
    private String lastName;
    private String dni;
    private String phone;
    private LocalDate birthDate;

    /**
     * Dirección postal. Modelada como string libre porque en este momento no
     * la usamos para geolocalización ni para validaciones estructuradas; un
     * VARCHAR es suficiente para el caso de uso de autoservicio del RF06.
     */
    private String address;

    /**
     * URL pública (o firmada) que apunta a la foto de perfil del usuario.
     * Modelamos sólo la URL porque en este PR no hay infraestructura de
     * almacenamiento de binarios (S3/MinIO): el frontend o un futuro adapter
     * se encargarán de subir el archivo y entregarnos la URL ya resuelta.
     */
    private String profilePictureUrl;

    private Instant createdAt;

    // Constructores
    public User() {
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public User(String email, String password, Role role, String firstName, String lastName, String dni, String phone, LocalDate birthDate) {
        this();
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dni = dni;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    // Comportamientos del Dominio Rico

    /**
     * Lógica de negocio para desactivar un usuario (baja lógica, RF05).
     */
    public void deactivate() {
        if (!this.isActive) {
            throw new IllegalStateException("El usuario ya está inactivo.");
        }
        this.isActive = false;
    }

    /**
     * Lógica de negocio para reactivar un usuario previamente dado de baja (RF05).
     * Permite revertir la baja lógica sin necesidad de recrear el registro y, por
     * lo tanto, sin perder el historial clínico/administrativo asociado.
     */
    public void activate() {
        if (this.isActive) {
            throw new IllegalStateException("El usuario ya está activo.");
        }
        this.isActive = true;
    }

    /**
     * Actualiza únicamente los campos del perfil que el administrador está
     * autorizado a modificar (RF05). El email, el DNI, el rol y la contraseña
     * quedan deliberadamente fuera de esta operación: cambiar el email/DNI
     * compromete unicidad y trazabilidad, y cambiar el rol o la contraseña
     * pertenece a flujos dedicados (recuperación o re-registro).
     */
    public void updateProfile(String firstName, String lastName, String phone, LocalDate birthDate) {
        if (firstName != null) {
            this.firstName = firstName;
        }
        if (lastName != null) {
            this.lastName = lastName;
        }
        // phone y birthDate son opcionales en el modelo: aceptamos null como
        // limpieza explícita sólo si el caller envió la clave; por simplicidad
        // los reemplazamos siempre, dejando al adapter REST la decisión de qué
        // mandar.
        this.phone = phone;
        this.birthDate = birthDate;
    }

    /**
     * Lógica de negocio para cambiar la contraseña.
     */
    public void changePassword(String currentPasswordHash, String newPasswordHash) {
        // if (!passwordEncoder.matches(currentPassword, this.password)) {
        //    throw new SecurityException("Contraseña actual incorrecta");
        // }
        this.password = newPasswordHash;
    }

    /**
     * Actualiza los datos modificables por el propio usuario autenticado
     * desde el flujo de autoservicio (RF06).
     *
     * Se separa deliberadamente de {@link #updateProfile} (RF05) porque la
     * autoservicio tiene reglas distintas a la administrativa:
     * <ul>
     *   <li>El usuario SÍ puede modificar su propio email — el chequeo de
     *       unicidad se delega al servicio de aplicación que tiene acceso al
     *       puerto del repositorio.</li>
     *   <li>El DNI sigue inmutable: es identificador funcional y de
     *       trazabilidad clínica, no debe poder reescribirse desde una API
     *       de perfil.</li>
     *   <li>Se admiten dos campos nuevos del RF06: {@code address} y
     *       {@code profilePictureUrl}.</li>
     * </ul>
     * Aceptar {@code null} para campos opcionales (teléfono, fecha de
     * nacimiento, dirección, foto) implica limpieza explícita: el adaptador
     * REST decide qué enviar y qué omitir.
     */
    public void updateSelfProfile(String email,
                                  String firstName,
                                  String lastName,
                                  String phone,
                                  LocalDate birthDate,
                                  String address,
                                  String profilePictureUrl) {
        if (email != null) {
            this.email = email;
        }
        if (firstName != null) {
            this.firstName = firstName;
        }
        if (lastName != null) {
            this.lastName = lastName;
        }
        // Los campos opcionales se sobreescriben siempre con el valor recibido
        // (incluido null) para permitir "limpiar" el dato desde la UI.
        this.phone = phone;
        this.birthDate = birthDate;
        this.address = address;
        this.profilePictureUrl = profilePictureUrl;
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
}