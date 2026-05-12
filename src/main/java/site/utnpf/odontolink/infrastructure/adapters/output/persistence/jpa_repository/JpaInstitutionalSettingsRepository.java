package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.InstitutionalSettingsEntity;

/**
 * Repositorio JPA de Spring Data para {@link InstitutionalSettingsEntity}.
 *
 * Sólo se exponen las operaciones heredadas de {@link JpaRepository}; al
 * tratarse de un singleton se accede siempre por el identificador fijo
 * ({@code SINGLETON_ID}), sin necesidad de queries adicionales.
 */
@Repository
public interface JpaInstitutionalSettingsRepository extends JpaRepository<InstitutionalSettingsEntity, Long> {
}
