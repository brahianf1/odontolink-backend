package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ProviderGuardrailEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaProviderGuardrailRepository extends JpaRepository<ProviderGuardrailEntity, Long> {

    List<ProviderGuardrailEntity> findAllByOrderByPriorityAscIdAsc();

    List<ProviderGuardrailEntity> findByAttachedTrueOrderByPriorityAscIdAsc();

    Optional<ProviderGuardrailEntity> findByProviderGuardrailUuid(String providerGuardrailUuid);
}
