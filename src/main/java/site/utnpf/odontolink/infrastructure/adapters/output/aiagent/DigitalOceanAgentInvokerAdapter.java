package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChatCompletionRequest;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChatCompletionResponse;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChoice;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoMessage;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoRetrievalBlock;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoRetrievalDocument;

import java.net.SocketTimeoutException;
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
 * del dashboard, distinta del Personal Access Token.
 *
 * <p>Detalles del wire format documentados oficialmente:
 * <ul>
 *   <li>Path: {@code /api/v1/chat/completions}.</li>
 *   <li><strong>Query string {@code ?agent=true} OBLIGATORIO.</strong> Sin el
 *       flag el endpoint no enruta al agente y queda esperando una request
 *       en formato distinto, lo que produce timeouts (incidente reproducido
 *       en produccion).</li>
 *   <li>Authorization: {@code Bearer <agent_access_key>}.</li>
 *   <li>Body minimo: {@code messages[]}. El campo {@code model} es opcional
 *       cuando se usa {@code agent=true}: el modelo lo provee el agente
 *       deployado. Soportamos override por property por si en algun entorno
 *       se necesita forzar uno.</li>
 *   <li>{@code stream:false} para modo sincronico (default nuestro).</li>
 * </ul>
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
    /** Query string obligatorio para enrutar al agente deployado en DO. */
    private static final String AGENT_FLAG = "agent=true";

    private final DigitalOceanGradientClient invocationClient;
    /**
     * Cliente dedicado al probe con read-timeout mas corto que el normal:
     * el {@code /health} no puede colgar 20s cuando el agente esta caido.
     */
    private final DigitalOceanGradientClient probeClient;
    /**
     * Nombre del modelo a forzar en el request. Si esta vacio, se omite del
     * body y el agente usa el modelo configurado en su dashboard.
     */
    private final String modelOverride;

    public DigitalOceanAgentInvokerAdapter(DigitalOceanGradientClient invocationClient,
                                           DigitalOceanGradientClient probeClient,
                                           String modelOverride) {
        this.invocationClient = invocationClient;
        this.probeClient = probeClient;
        this.modelOverride = modelOverride;
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
        DoChatCompletionRequest body = buildRequest(wire);
        long startedAt = System.currentTimeMillis();
        // INFO en cada invocacion: ayuda a correlacionar la request del FE con
        // la llamada externa cuando algo no anda. No incluye contenido del
        // mensaje (privacidad) ni el bearer (seguridad); solo metadata.
        log.info("Invocando agente DO: url={}, system+turns={}, model={}",
                fullUrl, wire.size(), modelOverride == null || modelOverride.isBlank() ? "<agent-default>" : modelOverride);
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
        } catch (ResourceAccessException ex) {
            // Errores de red (timeouts, conexion rechazada, DNS). El cause
            // tipico es SocketTimeoutException. Lo logueamos por separado del
            // 4xx/5xx del proveedor para que el operador distinga rapido entre
            // "DO me rechazo" vs "DO no respondio a tiempo".
            long elapsed = System.currentTimeMillis() - startedAt;
            log.warn("Timeout/red invocando agente DO: latencyMs={}, cause={}: {}",
                    elapsed, ex.getCause() == null ? "?" : ex.getCause().getClass().getSimpleName(),
                    ex.getMessage());
            throw new LlmProviderException(
                    "Timeout o falla de red invocando al agente IA.",
                    null, AiAgentErrorCodes.AI_PROVIDER_UNAVAILABLE, ex);
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
     * cached del circuito. Captura {@link LlmProviderException} y errores de
     * red para devolver un {@link ProbeResult} estructurado.
     */
    @Override
    public ProbeResult probe(String agentInvocationUrl) {
        if (agentInvocationUrl == null || agentInvocationUrl.isBlank()) {
            return new ProbeResult(false, "URL del agente no configurada.");
        }
        String fullUrl = buildCompletionsUrl(agentInvocationUrl);
        DoChatCompletionRequest body = buildRequest(List.of(new DoMessage("user", "ping")));
        try {
            probeClient.postAbsolute(fullUrl, body, DoChatCompletionResponse.class);
            return new ProbeResult(true, null);
        } catch (LlmProviderException ex) {
            String detail = "status=" + ex.getStatusCode() + " code=" + ex.getErrorCode()
                    + " message=" + ex.getMessage();
            log.warn("Probe del agente DO fallo: {}", detail);
            return new ProbeResult(false, detail);
        } catch (ResourceAccessException ex) {
            // Distinguimos timeout/red de error de parsing/aplicacion para que
            // el operador no se confunda — son problemas distintos.
            String causeName = ex.getCause() == null ? ex.getClass().getSimpleName()
                    : ex.getCause().getClass().getSimpleName();
            String detail = ex.getCause() instanceof SocketTimeoutException
                    ? "Timeout: el agente no respondio dentro del read-timeout. "
                            + "Verificar que la URL incluya ?agent=true (lo hace el adapter) y "
                            + "que la access key sea valida."
                    : "Falla de red: " + causeName + ": " + ex.getMessage();
            log.warn("Probe del agente DO fallo por red/timeout: {}", detail);
            return new ProbeResult(false, detail);
        } catch (RuntimeException ex) {
            // Cualquier otra falla (parsing, framework, etc). Mantenemos el
            // catch para que /health sea tolerante.
            log.warn("Probe del agente DO fallo con excepcion inesperada: {}",
                    ex.getMessage(), ex);
            return new ProbeResult(false, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * Compone la URL final agregando el path y el query string {@code agent=true}.
     * Manejamos el trailing slash y un eventual query string previo en la base
     * para evitar dobles {@code &} o {@code ?}.
     */
    private static String buildCompletionsUrl(String base) {
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String url = trimmed + COMPLETIONS_PATH;
        // Solo agregamos ?agent=true si el path todavia no lleva query.
        return url.contains("?") ? url + "&" + AGENT_FLAG : url + "?" + AGENT_FLAG;
    }

    /** Builder unico del body para que invoke y probe compartan exactamente la misma forma. */
    private DoChatCompletionRequest buildRequest(List<DoMessage> messages) {
        String model = (modelOverride == null || modelOverride.isBlank()) ? null : modelOverride;
        return new DoChatCompletionRequest(messages, false, model);
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
