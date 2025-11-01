package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SupervisorEntity;

import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para SupervisorEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 */
@Repository
public interface JpaSupervisorRepository extends JpaRepository<SupervisorEntity, Long> {
    Optional<SupervisorEntity> findByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
}
