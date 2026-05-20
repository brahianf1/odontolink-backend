package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia de usuarios (Hexagonal Architecture).
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);

    /**
     * Listado filtrado para el panel de administración (RF05).
     *
     * Los tres parámetros son opcionales (null = sin filtrar) y se combinan
     * con AND. El parámetro {@code query} busca de forma case-insensitive
     * sobre el nombre completo, el email y el DNI, lo que cubre las
     * búsquedas operativas más habituales del administrador.
     */
    List<User> findAllByFilters(Role role, Boolean isActive, String query);

    /**
     * Indica si existe otro usuario con el email dado, distinto al identificado
     * por {@code excludingId}. Es la versión "segura para actualizaciones" de
     * {@link #existsByEmail(String)}: permite validar unicidad sin acusar de
     * duplicado al propio usuario que se está modificando.
     */
    boolean existsByEmailAndIdNot(String email, Long excludingId);

    /**
     * Análogo a {@link #existsByEmailAndIdNot(String, Long)} pero sobre el DNI.
     */
    boolean existsByDniAndIdNot(String dni, Long excludingId);

    /**
     * Cuenta los usuarios con el rol indicado que están actualmente activos
     * ({@code isActive = true}).
     *
     * <p>Caso de uso principal: la regla "last-admin-standing" en la baja
     * lógica de administradores. Antes de desactivar un admin, el caso de
     * uso consulta este contador para garantizar que el sistema nunca
     * quede sin ninguna cuenta administrativa operativa — un lockout total
     * obligaría a intervenir la base de datos a mano y violaría el
     * principio de operación segura recomendado por OWASP ASVS.
     */
    long countActiveByRole(Role role);
}
