package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Guardrail;

import java.util.List;

/**
 * Puerto de entrada para el CRUD de guardrails clinicos del agente IA
 * (RF32). El admin define todos los guardrails desde el panel; el sistema
 * no provee textos por defecto.
 */
public interface IGuardrailAdminUseCase {

    List<Guardrail> listGuardrails();

    Guardrail getGuardrail(Long id);

    Guardrail createGuardrail(String label, String text, boolean active);

    Guardrail updateGuardrail(Long id, String label, String text, boolean active);

    Guardrail setGuardrailActive(Long id, boolean active);

    void deleteGuardrail(Long id);
}
