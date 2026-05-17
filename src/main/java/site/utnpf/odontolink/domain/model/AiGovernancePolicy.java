package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;

/**
 * Politica de gobernanza del modulo IA: los "candados" configurables que
 * controlan los pre-requisitos para publicar el agente (RF31).
 *
 * <p>Singleton: hay a lo sumo una fila. Mismo patron que
 * {@link InstitutionalSettings} y {@link AiAgentConfiguration}.
 *
 * <p>El administrador tiene la responsabilidad total de definir politica y
 * contenido; esta entidad le permite atarse las manos antes de un error
 * humano. Por ejemplo, prendiendo {@code requireGuardrails=true} y
 * {@code allowOverride=false}, se garantiza que ningun publish llegue a DO
 * sin guardrails activos. Si necesita publicar excepcionalmente sin alguno
 * de los requisitos, debe primero prender {@code allowOverride=true} en
 * esta entidad y luego invocar {@code POST /publish?override=true}: la
 * accion deliberada vs accidental.
 */
public class AiGovernancePolicy {

    public static final Long SINGLETON_ID = 1L;

    private Long id;
    /**
     * Si esta prendido, el publish exige al menos
     * {@link #minActiveGuardrails} guardrails activos (active=true).
     */
    private boolean requireGuardrails;
    private int minActiveGuardrails;
    /**
     * Si esta prendido, el publish exige que {@code systemPromptCore} no
     * este vacio. Es practicamente una invariante: sin prompt el LLM no
     * tiene rol definido.
     */
    private boolean requireSystemPrompt;
    /**
     * Si esta prendido, el publish exige que {@code welcomeMessage} no
     * este vacio.
     */
    private boolean requireWelcomeMessage;
    /**
     * Si esta prendido, el publish exige que exista al menos un documento
     * en la KB en estado INDEXED. Util cuando la clinica quiere garantizar
     * que el agente responda con RAG, no solo con su prompt.
     */
    private boolean requireIndexedDocuments;
    /**
     * Llave maestra: si esta apagada, ningun publish puede saltarse los
     * checks anteriores, incluso si el admin envia {@code override=true}.
     * Solo prendiendola desde este endpoint el admin habilita la
     * posibilidad de override por-publish.
     */
    private boolean allowOverride;
    private Instant updatedAt;

    public AiGovernancePolicy() {
    }

    public AiGovernancePolicy(Long id,
                              boolean requireGuardrails,
                              int minActiveGuardrails,
                              boolean requireSystemPrompt,
                              boolean requireWelcomeMessage,
                              boolean requireIndexedDocuments,
                              boolean allowOverride,
                              Instant updatedAt) {
        this.id = id;
        this.requireGuardrails = requireGuardrails;
        this.minActiveGuardrails = minActiveGuardrails;
        this.requireSystemPrompt = requireSystemPrompt;
        this.requireWelcomeMessage = requireWelcomeMessage;
        this.requireIndexedDocuments = requireIndexedDocuments;
        this.allowOverride = allowOverride;
        this.updatedAt = updatedAt;
    }

    /**
     * Politica restrictiva por defecto: exige guardrails + system prompt +
     * welcome, no permite override. El primer acceso del admin crea esta
     * fila para que el sistema empiece protegido; el admin puede aflojar
     * los toggles si lo necesita. Sin esto, el "default seguro" recaeria
     * en cada lector del codigo, lo que rompe la postura "el admin decide
     * todo el contenido": la *politica* (los toggles) es de admin, pero
     * la *postura inicial* del control es del sistema.
     */
    public static AiGovernancePolicy defaultStrict() {
        return new AiGovernancePolicy(
                SINGLETON_ID,
                true,
                1,
                true,
                true,
                false,
                false,
                Instant.now()
        );
    }

    public void apply(boolean requireGuardrails,
                      int minActiveGuardrails,
                      boolean requireSystemPrompt,
                      boolean requireWelcomeMessage,
                      boolean requireIndexedDocuments,
                      boolean allowOverride) {
        if (minActiveGuardrails < 0) {
            throw new InvalidBusinessRuleException("minActiveGuardrails no puede ser negativo.");
        }
        if (requireGuardrails && minActiveGuardrails < 1) {
            throw new InvalidBusinessRuleException(
                    "Si requireGuardrails esta prendido, minActiveGuardrails debe ser >= 1.");
        }
        this.requireGuardrails = requireGuardrails;
        this.minActiveGuardrails = minActiveGuardrails;
        this.requireSystemPrompt = requireSystemPrompt;
        this.requireWelcomeMessage = requireWelcomeMessage;
        this.requireIndexedDocuments = requireIndexedDocuments;
        this.allowOverride = allowOverride;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isRequireGuardrails() {
        return requireGuardrails;
    }

    public int getMinActiveGuardrails() {
        return minActiveGuardrails;
    }

    public boolean isRequireSystemPrompt() {
        return requireSystemPrompt;
    }

    public boolean isRequireWelcomeMessage() {
        return requireWelcomeMessage;
    }

    public boolean isRequireIndexedDocuments() {
        return requireIndexedDocuments;
    }

    public boolean isAllowOverride() {
        return allowOverride;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
