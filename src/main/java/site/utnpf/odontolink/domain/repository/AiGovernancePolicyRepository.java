package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AiGovernancePolicy;

import java.util.Optional;

/**
 * Puerto de salida para el singleton {@link AiGovernancePolicy} (RF31).
 */
public interface AiGovernancePolicyRepository {

    Optional<AiGovernancePolicy> findSingleton();

    AiGovernancePolicy save(AiGovernancePolicy policy);
}
