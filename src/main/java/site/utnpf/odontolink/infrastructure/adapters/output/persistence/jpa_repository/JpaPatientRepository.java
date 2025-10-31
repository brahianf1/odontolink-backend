package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;

import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para PatientEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 */
@Repository
public interface JpaPatientRepository extends JpaRepository<PatientEntity, Long> {
    Optional<PatientEntity> findByUserId(Long userId);
}
