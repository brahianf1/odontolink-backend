package site.utnpf.odontolink.domain.model;

/**
 * Perfil consistente para la administración del sistema.
 */
public class Administrator {
    private Long id;

    /** Relación 1-a-1: Un Admin ES un User */
    private User user;

    // (Puede tener campos futuros como 'permissionLevel', etc.)
}