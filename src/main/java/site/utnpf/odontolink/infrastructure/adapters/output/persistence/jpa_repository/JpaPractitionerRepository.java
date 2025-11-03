package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para PractitionerEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 */
@Repository
public interface JpaPractitionerRepository extends JpaRepository<PractitionerEntity, Long> {
    Optional<PractitionerEntity> findByUserId(Long userId);
    boolean existsByStudentId(String studentId);

    /**
     * Busca practicantes por nombre completo, DNI o legajo.
     * Implementa búsqueda flexible multi-campo con LIKE case-insensitive.
     */
    @Query("SELECT p FROM PractitionerEntity p JOIN p.user u " +
           "WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR u.dni LIKE CONCAT('%', :query, '%') " +
           "OR p.studentId LIKE CONCAT('%', :query, '%')")
    List<PractitionerEntity> searchByQuery(@Param("query") String query);
}
