package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;

import java.math.BigDecimal;

/**
 * Payload del PUT a {@code /api/admin/ai-agent/configuration}.
 *
 * <p>Bean Validation duplica las invariantes del dominio para devolver
 * 400 con detalle por campo antes de llegar al servicio (defensa en
 * profundidad). El servicio sigue validando.
 *
 * <p>Los guardrails NO viajan aqui: se administran via
 * {@code /api/admin/ai-agent/guardrails}. La instruccion final que viaja
 * al proveedor en {@code POST /publish} se compone con los guardrails
 * activos al momento del publish.
 */
public class UpdateAiAgentConfigurationRequestDTO {

    @NotBlank
    private String displayName;

    @NotBlank
    private String systemPromptCore;

    private String welcomeMessage;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal temperature;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal topP;

    @Min(1)
    @Max(512)
    private int maxTokens;

    @Min(1)
    @Max(50)
    private int k;

    @NotNull
    private AiRetrievalMethod retrievalMethod;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSystemPromptCore() { return systemPromptCore; }
    public void setSystemPromptCore(String systemPromptCore) { this.systemPromptCore = systemPromptCore; }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }

    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }

    public BigDecimal getTopP() { return topP; }
    public void setTopP(BigDecimal topP) { this.topP = topP; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getK() { return k; }
    public void setK(int k) { this.k = k; }

    public AiRetrievalMethod getRetrievalMethod() { return retrievalMethod; }
    public void setRetrievalMethod(AiRetrievalMethod retrievalMethod) { this.retrievalMethod = retrievalMethod; }
}
