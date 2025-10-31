package site.utnpf.odontolink.domain.model;

/**
 * Enumeración de los roles de usuario.
 * Define los niveles de autorización.
 */
public enum Role {
    ROLE_PATIENT,
    ROLE_PRACTITIONER, // "Practicante" (Estudiante)
    ROLE_SUPERVISOR,   // "Docente" (Supervisor)
    ROLE_ADMIN
}