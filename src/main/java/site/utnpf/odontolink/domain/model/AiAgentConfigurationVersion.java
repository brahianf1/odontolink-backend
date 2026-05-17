package site.utnpf.odontolink.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Snapshot inmutable de una publicacion del agente IA (RF31).
 *
 * <p>Cada {@code POST /publish} exitoso genera una version. El snapshot
 * guarda el contenido COMPLETO al momento del publish, incluyendo:
 * <ul>
 *   <li>el texto concatenado final ({@code composedInstruction}) que se envio
 *       al proveedor;</li>
 *   <li>los labels de los guardrails activos al publicar
 *       ({@code guardrailsLabelsSnapshot}, CSV), para auditoria
 *       independiente de la tabla viva de guardrails;</li>
 *   <li>los parametros numericos y el retrieval method;</li>
 *   <li>quien publico y si lo hizo con override + los requisitos que se
 *       saltearon (si los hubo).</li>
 * </ul>
 *
 * <p>Esta inmutabilidad permite implementar rollback: el admin selecciona
 * una version anterior y la sistema re-publica con los valores guardados,
 * independientemente de que despues se hayan borrado guardrails o
 * cambiado la configuracion.
 */
public class AiAgentConfigurationVersion {

    private Long id;
    private int versionNumber;
    private String displayName;
    private String systemPromptCore;
    private String welcomeMessage;
    private BigDecimal temperature;
    private BigDecimal topP;
    private int maxTokens;
    private int k;
    private AiRetrievalMethod retrievalMethod;
    /** Texto exacto que viajo a DO. Util para auditoria y para rollback fiel. */
    private String composedInstruction;
    /** CSV de labels de guardrails activos al momento del publish. */
    private String guardrailsLabelsSnapshot;
    private Long publishedByUserId;
    private boolean publishedWithOverride;
    /** CSV de requisitos que se saltearon (si publishedWithOverride=true). */
    private String missingRequirementsAtPublish;
    private Instant publishedAt;

    public AiAgentConfigurationVersion() {
    }

    public AiAgentConfigurationVersion(Long id,
                                       int versionNumber,
                                       String displayName,
                                       String systemPromptCore,
                                       String welcomeMessage,
                                       BigDecimal temperature,
                                       BigDecimal topP,
                                       int maxTokens,
                                       int k,
                                       AiRetrievalMethod retrievalMethod,
                                       String composedInstruction,
                                       String guardrailsLabelsSnapshot,
                                       Long publishedByUserId,
                                       boolean publishedWithOverride,
                                       String missingRequirementsAtPublish,
                                       Instant publishedAt) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.displayName = displayName;
        this.systemPromptCore = systemPromptCore;
        this.welcomeMessage = welcomeMessage;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.k = k;
        this.retrievalMethod = retrievalMethod;
        this.composedInstruction = composedInstruction;
        this.guardrailsLabelsSnapshot = guardrailsLabelsSnapshot;
        this.publishedByUserId = publishedByUserId;
        this.publishedWithOverride = publishedWithOverride;
        this.missingRequirementsAtPublish = missingRequirementsAtPublish;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersionNumber() {
        return versionNumber;
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

    public String getComposedInstruction() {
        return composedInstruction;
    }

    public String getGuardrailsLabelsSnapshot() {
        return guardrailsLabelsSnapshot;
    }

    public Long getPublishedByUserId() {
        return publishedByUserId;
    }

    public boolean isPublishedWithOverride() {
        return publishedWithOverride;
    }

    public String getMissingRequirementsAtPublish() {
        return missingRequirementsAtPublish;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
