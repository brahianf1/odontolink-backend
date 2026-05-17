package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.GuardrailEntity;

import java.util.List;

@Repository
public interface JpaGuardrailRepository extends JpaRepository<GuardrailEntity, Long> {

    List<GuardrailEntity> findAllByOrderByCreatedAtAsc();

    List<GuardrailEntity> findByActiveTrueOrderByCreatedAtAsc();

    long countByActiveTrue();
}
