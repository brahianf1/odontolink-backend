package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChatCompletionRequest;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChatCompletionResponse;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChoice;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoMessage;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoRetrievalBlock;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoRetrievalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de invocacion del agente IA (chat completions) contra DigitalOcean
 * Gradient (RF29/RF34).
 *
 * <p>Usa un {@link DigitalOceanGradientClient} <strong>dedicado al endpoint del
 * agente</strong>, cableado con la access key especifica del agente
 * (propiedad {@code odontolink.ai-agent.agent-invocation-access-key}). Es
 * critico que NO se use el mismo cliente que el management API: cada agente
 * deployado en DO Gradient genera una key propia en su seccion "Endpoint Keys"
 * del dashboard, distinta del Personal Access Token. Enviar el PAT al endpoint
 * del agente devuelve 401 sin body — el bug original que motivo este adapter.
 *
 * <p>La resiliencia se modela con Resilience4j:
 * <ul>
 *   <li>{@code @CircuitBreaker(name="doAgentInvoke")}: tras N fallas el
 *       circuito abre y las siguientes invocaciones devuelven excepcion
 *       inmediata, permitiendo que el use case ejecute el fallback amigable
 *       en milisegundos en lugar de esperar timeouts del proveedor caido.</li>
 *   <li>{@code @Retry(name="doAgentInvoke")}: reintenta excepciones de red
 *       transitorias (SocketTimeoutException, etc.) un par de veces con
 *       backoff antes de declararse fallido.</li>
 *   <li>NO usamos {@code @TimeLimiter} porque solo funciona con metodos
 *       async (CompletableFuture/reactive) y nuestro RestClient es sincronico.
 *       Los timeouts del HTTP los pone el {@code RestClient} subyacente
 *       (connect/read configurados en properties).</li>
 * </ul>
 *
 * <p>El metodo de fallback DEBE estar en la misma clase y tener la misma
 * firma + un parametro {@code Throwable} al final. Lo usa Resilience4j cuando
 * el circuito esta abierto o cuando se agotaron los retries.
 */
public class DigitalOceanAgentInvokerAdapter implements ILlmAgentInvokerPort {

    private static final Logger log = LoggerFactory.getLogger(DigitalOceanAgentInvokerAdapter.class);
    private static final String COMPLETIONS_PATH = "/api/v1/chat/completions";

    private final DigitalOceanGradientClient invocationClient;

    public DigitalOceanAgentInvokerAdapter(DigitalOceanGradientClient invocationClient) {
        this.invocationClient = invocationClient;
    }

    @Override
    @CircuitBreaker(name = "doAgentInvoke", fallbackMethod = "invokeFallback")
    @Retry(name = "doAgentInvoke")
    public AgentInvocationResult invoke(String agentInvocationUrl, List<ChatMessage> messages) {
        if (agentInvocationUrl == null || agentInvocationUrl.isBlank()) {
            throw new LlmProviderException(
                    "URL del agente vacia.", null, AiAgentErrorCodes.AI_AGENT_INVOCATION_URL_UNAVAILABLE);
        }
        String fullUrl = buildCompletionsUrl(agentInvocationUrl);
        List<DoMessage> wire = new ArrayList<>(messages.size());
        for (ChatMessage m : messages) {
            wire.add(new DoMessage(m.role(), m.content()));
        }
        DoChatCompletionRequest body = new DoChatCompletionRequest(wire, false, Boolean.TRUE);
        long startedAt = System.currentTimeMillis();
        // INFO en cada invocacion: ayuda a correlacionar la request del FE con
        // la llamada externa cuando algo no anda. No incluye contenido del
        // mensaje (privacidad) ni el bearer (seguridad); solo metadata.
        log.info("Invocando agente DO: url={}, system+turns={}", fullUrl, wire.size());
        try {
            DoChatCompletionResponse response = invocationClient.postAbsolute(
                    fullUrl, body, DoChatCompletionResponse.class);
            long elapsed = System.currentTimeMillis() - startedAt;
            int choices = response == null || response.choices() == null ? 0 : response.choices().size();
            int retrieved = response == null || response.retrieval() == null
                    || response.retrieval().retrievedData() == null
                    ? 0 : response.retrieval().retrievedData().size();
            log.info("Respuesta agente DO: latencyMs={}, choices={}, retrieved={}",
                    elapsed, choices, retrieved);
            return toResult(response);
        } catch (LlmProviderException ex) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.warn("Falla invocando agente DO: latencyMs={}, code={}, message={}",
                    elapsed, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Fallback invocado por Resilience4j cuando el circuito esta abierto o se
     * agotaron los retries. Re-lanza {@link LlmProviderException} para que el
     * use case lo capture y arme la respuesta fallback amigable. NO devolvemos
     * un AgentInvocationResult vacio porque el use case ya tiene logica
     * dedicada para fallback y queremos un unico camino de codigo.
     */
    @SuppressWarnings("unused") // referenciado por @CircuitBreaker
    public AgentInvocationResult invokeFallback(String agentInvocationUrl,
                                                List<ChatMessage> messages,
                                                Throwable cause) {
        log.warn("Fallback de Resilience4j en invocacion del agente (url={}, msgs={}): {} ({})",
                agentInvocationUrl, messages == null ? 0 : messages.size(),
                cause.getMessage(), cause.getClass().getSimpleName());
        if (cause instanceof LlmProviderException llmEx) {
            throw llmEx;
        }
        throw new LlmProviderException(
                "Servicio de IA no disponible.", null,
                AiAgentErrorCodes.AI_PROVIDER_UNAVAILABLE, cause);
    }

    /**
     * Invocacion minima para health-check. NO se anota con {@code @CircuitBreaker}
     * a proposito: el health debe ver el estado real del proveedor, no el
     * cached del circuito. Captura {@link LlmProviderException} para
     * convertirla en un {@link ProbeResult} estructurado.
     */
    @Override
    public ProbeResult probe(String agentInvocationUrl) {
        if (agentInvocationUrl == null || agentInvocationUrl.isBlank()) {
            return new ProbeResult(false, "URL del agente no configurada.");
        }
        String fullUrl = buildCompletionsUrl(agentInvocationUrl);
        DoChatCompletionRequest body = new DoChatCompletionRequest(
                List.of(new DoMessage("user", "ping")),
                false,
                Boolean.FALSE
        );
        try {
            invocationClient.postAbsolute(fullUrl, body, DoChatCompletionResponse.class);
            return new ProbeResult(true, null);
        } catch (LlmProviderException ex) {
            String detail = "status=" + ex.getStatusCode() + " code=" + ex.getErrorCode()
                    + " message=" + ex.getMessage();
            log.warn("Probe del agente DO fallo: {}", detail);
            return new ProbeResult(false, detail);
        } catch (RuntimeException ex) {
            // Capturamos red/timeout/cualquier otra falla para no propagar a
            // /health (debe ser un endpoint tolerante).
            log.warn("Probe del agente DO fallo con excepcion no-LLM: {}",
                    ex.getMessage(), ex);
            return new ProbeResult(false, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * Compone la URL final concatenando el path estable al base URL del agente.
     * Manejamos el trailing slash para evitar dobles barras.
     */
    private static String buildCompletionsUrl(String base) {
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return trimmed + COMPLETIONS_PATH;
    }

    private static AgentInvocationResult toResult(DoChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return new AgentInvocationResult("", List.of(), 0);
        }
        DoChoice first = response.choices().get(0);
        String reply = first.message() == null ? "" : first.message().content();
        List<RetrievalDocument> docs = new ArrayList<>();
        DoRetrievalBlock retrieval = response.retrieval();
        if (retrieval != null && retrieval.retrievedData() != null) {
            for (DoRetrievalDocument d : retrieval.retrievedData()) {
                if (d == null) {
                    continue;
                }
                double score = d.score() == null ? 0.0 : d.score();
                docs.add(new RetrievalDocument(d.dataSourceUuid(), score));
            }
        }
        int totalTokens = response.usage() != null && response.usage().totalTokens() != null
                ? response.usage().totalTokens() : 0;
        return new AgentInvocationResult(reply, docs, totalTokens);
    }
}
