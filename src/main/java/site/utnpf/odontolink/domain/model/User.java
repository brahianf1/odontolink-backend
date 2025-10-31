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

    private Instant createdAt;
    
    // Comportamientos del Dominio Rico
    
    /**
     * Lógica de negocio para desactivar un usuario.
     */
    public void deactivate() {
        if (!this.isActive) {
            throw new IllegalStateException("El usuario ya está inactivo.");
        }
        this.isActive = false;
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
}