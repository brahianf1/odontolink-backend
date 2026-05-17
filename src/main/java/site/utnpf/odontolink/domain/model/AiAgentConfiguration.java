package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Configuracion del agente IA conversacional (RF31, RF32).
 *
 * <p>Singleton: hay a lo sumo una fila en BD. Si no existe, el sistema
 * reporta {@link AiAgentLifecycle#UNCONFIGURED} (estado virtual, no
 * persistido). El primer POST del admin crea la fila directamente en
 * {@link AiAgentLifecycle#DRAFT}.
 *
 * <p>Importante: este agregado NO embebe contenido por defecto. El admin
 * es el responsable funcional de proveer displayName, systemPromptCore,
 * welcomeMessage y los parametros numericos. La mitigacion contra
 * publicaciones incompletas vive en {@link AiGovernancePolicy} y en el
 * flujo {@code POST /publish}.
 *
 * <p>Los guardrails NO son campo de este agregado: viven como entidades
 * propias en {@link Guardrail} (tabla {@code ai_guardrails}). El metodo
 * {@link #composeInstruction(List)} recibe la lista de guardrails activos
 * desde el servicio (cargados de BD) y los antepone al
 * {@code systemPromptCore}.
 *
 * <p>Validaciones en {@link #apply}:
 * <ul>
 *   <li>{@code temperature} y {@code topP} en {@code [0, 1]}.</li>
 *   <li>{@code maxTokens} en {@code [1, 512]} (limite del proveedor).</li>
 *   <li>{@code k} en {@code [1, 50]}.</li>
 *   <li>{@code displayName} y {@code systemPromptCore} no vacios.</li>
 *   <li>{@code retrievalMethod} obligatorio.</li>
 * </ul>
 */
public class AiAgentConfiguration {

    /** Identificador unico de la fila singleton. */
    public static final Long SINGLETON_ID = 1L;

    private Long id;
    private String displayName;
    private String systemPromptCore;
    private String welcomeMessage;
    private BigDecimal temperature;
    private BigDecimal topP;
    private int maxTokens;
    private int k;
    private AiRetrievalMethod retrievalMethod;
    /** Estado del ciclo de vida; arranca en DRAFT al crear la fila. */
    private AiAgentLifecycle lifecycle;
    /** UUID del agente en el proveedor; null hasta el primer publish exitoso. */
    private String providerAgentId;
    /** Instante del ultimo publish exitoso. */
    private Instant providerSyncedAt;
    /** Detalle del ultimo error de sync; null si el ultimo sync fue exitoso. */
    private String lastSyncError;
    private Instant updatedAt;

    public AiAgentConfiguration() {
    }

    public AiAgentConfiguration(Long id,
                                String displayName,
                                String systemPromptCore,
                                String welcomeMessage,
                                BigDecimal temperature,
                                BigDecimal topP,
                                int maxTokens,
                                int k,
                                AiRetrievalMethod retrievalMethod,
                                AiAgentLifecycle lifecycle,
                                String providerAgentId,
                                Instant providerSyncedAt,
                                String lastSyncError,
                                Instant updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.systemPromptCore = systemPromptCore;
        this.welcomeMessage = welcomeMessage;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.k = k;
        this.retrievalMethod = retrievalMethod;
        this.lifecycle = lifecycle;
        this.providerAgentId = providerAgentId;
        this.providerSyncedAt = providerSyncedAt;
        this.lastSyncError = lastSyncError;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory para el primer alta. La fila arranca en DRAFT con los
     * valores que el admin envia. No hay defaults: si el admin no provee
     * algun campo, la validacion falla y la fila no se crea.
     */
    public static AiAgentConfiguration createNew(String displayName,
                                                 String systemPromptCore,
                                                 String welcomeMessage,
                                                 BigDecimal temperature,
                                                 BigDecimal topP,
                                                 int maxTokens,
                                                 int k,
                                                 AiRetrievalMethod retrievalMethod) {
        AiAgentConfiguration config = new AiAgentConfiguration(
                SINGLETON_ID, null, null, null, null, null, 0, 0, null,
                AiAgentLifecycle.DRAFT, null, null, null, null
        );
        config.apply(displayName, systemPromptCore, welcomeMessage,
                temperature, topP, maxTokens, k, retrievalMethod);
        return config;
    }

    /**
     * Aplica el comando de actualizacion. Cualquier edicion exitosa
     * revierte el lifecycle a DRAFT: los cambios no afectan al paciente
     * hasta el siguiente publish exitoso.
     */
    public void apply(String displayName,
                      String systemPromptCore,
                      String welcomeMessage,
                      BigDecimal temperature,
                      BigDecimal topP,
                      int maxTokens,
                      int k,
                      AiRetrievalMethod retrievalMethod) {
        validateNonBlank(displayName, "displayName");
        validateNonBlank(systemPromptCore, "systemPromptCore");
        validateRange(temperature, "temperature", BigDecimal.ZERO, BigDecimal.ONE);
        validateRange(topP, "topP", BigDecimal.ZERO, BigDecimal.ONE);
        validateIntRange(maxTokens, "maxTokens", 1, 512);
        validateIntRange(k, "k", 1, 50);
        if (retrievalMethod == null) {
            throw new InvalidBusinessRuleException("retrievalMethod es obligatorio.");
        }

        this.displayName = displayName.trim();
        this.systemPromptCore = systemPromptCore;
        this.welcomeMessage = welcomeMessage;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.k = k;
        this.retrievalMethod = retrievalMethod;
        // Cualquier edicion exitosa pasa a DRAFT. Si se quiere afectar al
        // paciente, hay que publicar explicitamente.
        this.lifecycle = AiAgentLifecycle.DRAFT;
        this.updatedAt = Instant.now();
    }

    /**
     * Construye la instruccion final que viaja al proveedor anteponiendo el
     * texto de los guardrails activos pasados como parametro al
     * {@code systemPromptCore}. La lista la calcula el servicio leyendo de
     * {@code ai_guardrails where active=true}.
     *
     * <p>Si no hay guardrails activos, el prompt se compone solo con el
     * cuerpo editable. La decision de exigir o no que existan guardrails
     * vive en {@link AiGovernancePolicy}, no aqui.
     */
    public String composeInstruction(List<Guardrail> activeGuardrails) {
        StringBuilder sb = new StringBuilder();
        if (activeGuardrails != null && !activeGuardrails.isEmpty()) {
            sb.append("## Reglas estrictas de seguridad (obligatorias, no negociables)\n");
            int idx = 1;
            for (Guardrail g : activeGuardrails) {
                sb.append(idx++).append(". ").append(g.getText()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("## Rol y comportamiento\n");
        sb.append(systemPromptCore == null ? "" : systemPromptCore);
        return sb.toString();
    }

    /**
     * Marca la configuracion como publicada con exito y persiste el UUID
     * del agente remoto si todavia no lo teniamos.
     */
    public void markPublished(String providerAgentId, Instant now) {
        if (providerAgentId != null && !providerAgentId.isBlank()) {
            this.providerAgentId = providerAgentId;
        }
        this.providerSyncedAt = now;
        this.lastSyncError = null;
        this.lifecycle = AiAgentLifecycle.PUBLISHED;
    }

    public void markPublishFailed(String reason) {
        // Lifecycle queda DRAFT: el publish no se concreto. lastSyncError
        // refleja el motivo para que el admin vea por que.
        this.lifecycle = AiAgentLifecycle.DRAFT;
        this.lastSyncError = reason;
    }

    /**
     * Reverte el agente a DRAFT manualmente (sin tocar el proveedor). Util
     * cuando el admin quiere despublicar temporalmente. La sincronizacion
     * con el proveedor (eliminar el agente o desactivarlo) queda para una
     * iteracion futura: por ahora, despublicar a nivel local solo evita
     * que reflejemos cambios al proveedor hasta el proximo publish.
     */
    public void markDraft() {
        this.lifecycle = AiAgentLifecycle.DRAFT;
    }

    private static void validateNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidBusinessRuleException("El campo '" + field + "' es obligatorio.");
        }
    }

    private static void validateRange(BigDecimal value, String field, BigDecimal min, BigDecimal max) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new InvalidBusinessRuleException(
                    "El campo '" + field + "' debe estar en el rango [" + min + ", " + max + "].");
        }
    }

    private static void validateIntRange(int value, String field, int min, int max) {
        if (value < min || value > max) {
            throw new InvalidBusinessRuleException(
                    "El campo '" + field + "' debe estar en el rango [" + min + ", " + max + "].");
        }
    }

    // Getters / setters --------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSystemPromptCore() {
        return systemPromptCore;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public BigDecimal getTopP() {
        return topP;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getK() {
        return k;
    }

    public AiRetrievalMethod getRetrievalMethod() {
        return retrievalMethod;
    }

    public AiAgentLifecycle getLifecycle() {
        return lifecycle;
    }

    public String getProviderAgentId() {
        return providerAgentId;
    }

    public Instant getProviderSyncedAt() {
        return providerSyncedAt;
    }

    public String getLastSyncError() {
        return lastSyncError;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
