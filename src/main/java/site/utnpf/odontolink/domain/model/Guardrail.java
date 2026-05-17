package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;

/**
 * Regla de seguridad (guardrail) inyectada al prompt del agente IA (RF32).
 *
 * <p>A diferencia de la version inicial (enum con textos hardcodeados), los
 * guardrails ahora son entidades de BD editables por el administrador desde
 * el panel. Esto permite a la clinica ajustar las politicas de seguridad
 * sin requerir un PR y un deploy; el rol ADMIN es el responsable funcional
 * de definirlas.
 *
 * <p>El sistema NO embebe textos clinicos por defecto. La mitigacion contra
 * "publicar sin guardrails" vive en {@link AiGovernancePolicy} (toggles
 * configurables que el admin tambien controla) y en el flujo
 * {@code POST /publish} (corre los checks antes de sincronizar con el
 * proveedor).
 */
public class Guardrail {

    private Long id;
    /**
     * Etiqueta corta visible en el panel (e.g. "Sin diagnostico clinico",
     * "Redirigir emergencias"). No se envia al proveedor; sirve al admin
     * para identificar la regla.
     */
    private String label;
    /**
     * Texto exacto que se inyecta en el prompt. Se permite multi-linea y
     * contenido extenso porque puede explicar la regla con detalle.
     */
    private String text;
    /**
     * Si esta activo, {@link AiAgentConfiguration#composeInstruction} lo
     * incluye en la concatenacion final del prompt. Permite al admin
     * desactivar temporalmente un guardrail sin borrarlo.
     */
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Guardrail() {
    }

    public Guardrail(Long id,
                     String label,
                     String text,
                     boolean active,
                     Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.label = label;
        this.text = text;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Guardrail createNew(String label, String text, boolean active) {
        validateLabel(label);
        validateText(text);
        Instant now = Instant.now();
        return new Guardrail(null, label.trim(), text, active, now, now);
    }

    public void apply(String label, String text, boolean active) {
        validateLabel(label);
        validateText(text);
        this.label = label.trim();
        this.text = text;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            this.updatedAt = Instant.now();
        }
    }

    private static void validateLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'label' del guardrail es obligatorio.");
        }
        if (label.length() > 100) {
            throw new InvalidBusinessRuleException("El campo 'label' del guardrail no puede exceder 100 caracteres.");
        }
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'text' del guardrail es obligatorio.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
