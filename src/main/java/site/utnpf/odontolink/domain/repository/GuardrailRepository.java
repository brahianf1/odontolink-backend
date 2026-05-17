package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Guardrail;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para los guardrails (reglas de seguridad) del agente IA
 * (RF32).
 */
public interface GuardrailRepository {

    List<Guardrail> findAllOrderByCreatedAtAsc();

    List<Guardrail> findAllActiveOrderByCreatedAtAsc();

    Optional<Guardrail> findById(Long id);

    Guardrail save(Guardrail guardrail);

    void deleteById(Long id);

    long countActive();
}
