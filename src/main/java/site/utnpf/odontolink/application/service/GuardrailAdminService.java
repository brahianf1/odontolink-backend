package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IGuardrailAdminUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.GuardrailRepository;

import java.util.List;

/**
 * Servicio de aplicacion para el CRUD de guardrails (RF32).
 *
 * <p>Cualquier modificacion (alta, baja, cambio de active) revierte la
 * configuracion vigente del agente a DRAFT, garantizando que el cambio en
 * las reglas de seguridad no afecte al paciente hasta el siguiente publish
 * explicito. Sin esto, el admin podria desactivar un guardrail y el
 * cambio se reflejaria en el proveedor solo al siguiente save de
 * configuracion, lo que es confuso y peligroso.
 */
@Transactional
public class GuardrailAdminService implements IGuardrailAdminUseCase {

    private final GuardrailRepository guardrailRepository;
    private final AiAgentConfigurationRepository configRepository;

    public GuardrailAdminService(GuardrailRepository guardrailRepository,
                                 AiAgentConfigurationRepository configRepository) {
        this.guardrailRepository = guardrailRepository;
        this.configRepository = configRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Guardrail> listGuardrails() {
        return guardrailRepository.findAllOrderByCreatedAtAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Guardrail getGuardrail(Long id) {
        return guardrailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guardrail", "id", String.valueOf(id)));
    }

    @Override
    public Guardrail createGuardrail(String label, String text, boolean active) {
        Guardrail guardrail = Guardrail.createNew(label, text, active);
        Guardrail saved = guardrailRepository.save(guardrail);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    @Override
    public Guardrail updateGuardrail(Long id, String label, String text, boolean active) {
        Guardrail guardrail = getGuardrail(id);
        guardrail.apply(label, text, active);
        Guardrail saved = guardrailRepository.save(guardrail);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    @Override
    public Guardrail setGuardrailActive(Long id, boolean active) {
        Guardrail guardrail = getGuardrail(id);
        guardrail.setActive(active);
        Guardrail saved = guardrailRepository.save(guardrail);
        markConfigurationAsDraftIfPublished();
        return saved;
    }

    @Override
    public void deleteGuardrail(Long id) {
        if (guardrailRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Guardrail", "id", String.valueOf(id));
        }
        guardrailRepository.deleteById(id);
        markConfigurationAsDraftIfPublished();
    }

    /**
     * Si la configuracion del agente esta PUBLISHED, la baja a DRAFT.
     * Cualquier cambio en el catalogo de guardrails altera el prompt final
     * efectivo y el admin debe confirmar la nueva combinacion via publish.
     */
    private void markConfigurationAsDraftIfPublished() {
        configRepository.findSingleton().ifPresent(config -> {
            if (config.getLifecycle() == site.utnpf.odontolink.domain.model.AiAgentLifecycle.PUBLISHED) {
                config.markDraft();
                configRepository.save(config);
            }
        });
    }
}
