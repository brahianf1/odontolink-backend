package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;

/**
 * Repositorio JPA para TreatmentEntity.
 * Proporciona operaciones CRUD básicas y consultas personalizadas.
 */
public interface JpaTreatmentRepository extends JpaRepository<TreatmentEntity, Long> {

    /**
     * Verifica si existe un tratamiento con el nombre dado.
     */
    boolean existsByName(String name);
}
