package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;

import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para PractitionerEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 */
@Repository
public interface JpaPractitionerRepository extends JpaRepository<PractitionerEntity, Long> {
    Optional<PractitionerEntity> findByUserId(Long userId);
    boolean existsByStudentId(String studentId);
}
