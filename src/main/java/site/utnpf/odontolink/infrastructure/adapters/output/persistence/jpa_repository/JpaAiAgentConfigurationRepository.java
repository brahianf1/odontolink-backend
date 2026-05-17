package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationEntity;

/**
 * Repositorio JPA de Spring Data para {@link AiAgentConfigurationEntity}.
 *
 * <p>Singleton: solo se accede por el ID fijo del agregado. No se exponen
 * queries adicionales.
 */
@Repository
public interface JpaAiAgentConfigurationRepository extends JpaRepository<AiAgentConfigurationEntity, Long> {
}
