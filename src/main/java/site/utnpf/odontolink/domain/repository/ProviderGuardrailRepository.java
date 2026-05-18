package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ProviderGuardrail;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para los guardrails nativos del proveedor (RF31).
 *
 * <p>El espejo local guarda la <strong>intencion del admin</strong> (attached
 * + priority). La fuente de verdad de "lo que esta vigente en el proveedor"
 * sigue siendo el proveedor mismo; se reconcilia en cada {@code publish()}.
 */
public interface ProviderGuardrailRepository {

    List<ProviderGuardrail> findAllOrderByPriorityAsc();

    List<ProviderGuardrail> findAllAttachedOrderByPriorityAsc();

    Optional<ProviderGuardrail> findById(Long id);

    Optional<ProviderGuardrail> findByProviderGuardrailUuid(String uuid);

    ProviderGuardrail save(ProviderGuardrail guardrail);

    void deleteById(Long id);
}
