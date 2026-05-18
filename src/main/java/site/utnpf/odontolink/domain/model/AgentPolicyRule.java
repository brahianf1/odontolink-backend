package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;

/**
 * Regla de comportamiento del agente IA (RF31, RF32) — instruccion de texto en
 * lenguaje natural curada por el admin clinico que el LLM debe respetar.
 *
 * <p><b>Concepto clave</b>: estas reglas NO son guardrails en sentido estricto
 * (procesadores binarios que filtran inputs/outputs). Son <strong>politicas
 * de comportamiento</strong> expresadas como texto que se concatenan al
 * {@code systemPromptCore} del agente al hacer {@code publish()}. El LLM las
 * respeta porque viven en el system prompt.
 *
 * <p>Ejemplos validos:
 * <ul>
 *   <li>"No emitir diagnosticos clinicos. Si el usuario describe sintomas,
 *       sugerir agendar un turno con un practicante."</li>
 *   <li>"Ante palabras como dolor agudo, sangrado, traumatismo, indicar que
 *       el usuario debe contactar el numero de emergencia."</li>
 *   <li>"No recomendar medicamentos, dosis ni tratamientos farmacologicos."</li>
 * </ul>
 *
 * <p><b>Por que se llama PolicyRule y no Guardrail</b>: el dashboard de
 * DigitalOcean Gradient tiene una seccion "Guardrails" que son recursos de
 * primera clase (jailbreak / sensitive_data / content_moderation) — esos viven
 * en {@link ProviderGuardrail}. Nuestras "policy rules" son un concepto distinto
 * y agnostico al proveedor: cualquier LLM con system prompt las soporta.
 *
 * <p>Si en el futuro migramos a otro proveedor (Anthropic, Bedrock, Vertex),
 * estas reglas siguen funcionando idEnticas porque todos los LLMs respetan
 * instrucciones en el system message.
 *
 * <p>El sistema NO embebe textos clinicos por defecto. La mitigacion contra
 * "publicar sin reglas" vive en {@link AiGovernancePolicy} (toggles
 * configurables que el admin tambien controla) y en el flujo
 * {@code POST /publish}.
 */
public class AgentPolicyRule {

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
     * Si esta activa, {@link AiAgentConfiguration#composeInstruction} la
     * incluye en la concatenacion final del prompt. Permite al admin
     * desactivar temporalmente una regla sin borrarla.
     */
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public AgentPolicyRule() {
    }

    public AgentPolicyRule(Long id,
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

    public static AgentPolicyRule createNew(String label, String text, boolean active) {
        validateLabel(label);
        validateText(text);
        Instant now = Instant.now();
        return new AgentPolicyRule(null, label.trim(), text, active, now, now);
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
            throw new InvalidBusinessRuleException("El campo 'label' de la regla es obligatorio.");
        }
        if (label.length() > 100) {
            throw new InvalidBusinessRuleException("El campo 'label' de la regla no puede exceder 100 caracteres.");
        }
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'text' de la regla es obligatorio.");
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
