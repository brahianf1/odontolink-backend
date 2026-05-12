package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para UserEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByDniAndIdNot(String dni, Long id);

    /**
     * Consulta dinámica para el listado administrativo de usuarios (RF05).
     *
     * Cada filtro es opcional: cuando un parámetro llega en {@code null}, su
     * cláusula se desactiva mediante la comparación {@code :param IS NULL OR ...}.
     * El parámetro {@code query} se aplica con LIKE case-insensitive sobre el
     * email, el DNI y la concatenación de nombre + apellido, lo que cubre los
     * tres modos de búsqueda que usa el administrador.
     *
     * Se ordena por ID descendente para que los usuarios creados más
     * recientemente aparezcan primero, lo cual es coherente con el flujo
     * operativo del panel.
     */
    @Query("""
            SELECT u FROM UserEntity u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:isActive IS NULL OR u.isActive = :isActive)
              AND (
                    :query IS NULL OR :query = ''
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                  )
            ORDER BY u.id DESC
            """)
    List<UserEntity> findAllByFilters(@Param("role") Role role,
                                      @Param("isActive") Boolean isActive,
                                      @Param("query") String query);
}
