package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP centralizado para la API de DigitalOcean Gradient AI.
 *
 * <p>Centraliza tres responsabilidades:
 * <ol>
 *   <li>Inyectar el header {@code Authorization: Bearer ...} en cada llamada
 *       (cableado en el {@link RestClient} subyacente).</li>
 *   <li>Mapear cualquier 4xx/5xx a {@link LlmProviderException} con el
 *       {@code errorCode} adecuado para que el {@code GlobalExceptionHandler}
 *       traduzca a HTTP 503 con un payload limpio para el cliente del backend.</li>
 *   <li>Recortar el body de error a 500 chars en logs para evitar volcados
 *       descontrolados de respuestas grandes.</li>
 * </ol>
 *
 * <p>El {@link RestClient} subyacente se construye en
 * {@code AiAgentBeanConfiguration} con timeouts especificos del modulo.
 */
public class DigitalOceanGradientClient {

    private static final Logger log = LoggerFactory.getLogger(DigitalOceanGradientClient.class);
    private static final int MAX_ERROR_BODY_CHARS = 500;

    private final RestClient restClient;

    public DigitalOceanGradientClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <R> R get(String path, Class<R> responseType) {
        return restClient.get()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(responseType);
    }

    public <R> R post(String path, Object body, Class<R> responseType) {
        return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(responseType);
    }

    public <R> R put(String path, Object body, Class<R> responseType) {
        return restClient.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(responseType);
    }

    public void delete(String path) {
        restClient.delete()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .toBodilessEntity();
    }

    /**
     * Traduce cualquier respuesta 4xx/5xx del proveedor en una
     * {@link LlmProviderException} tipada. Es invocado por el contrato
     * {@code onStatus(...)} de {@link RestClient}, asi que debe respetar la
     * firma {@code (HttpRequest, ClientHttpResponse) throws IOException}.
     */
    private void translateError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        byte[] bytes = response.getBody().readAllBytes();
        String body = new String(bytes, StandardCharsets.UTF_8);
        String snippet = body.length() > MAX_ERROR_BODY_CHARS
                ? body.substring(0, MAX_ERROR_BODY_CHARS) + "..."
                : body;

        String requestId = response.getHeaders().getFirst("x-request-id");
        log.warn("DigitalOcean Gradient {} {} respondio {} (request-id={}): {}",
                request.getMethod(), request.getURI(), status.value(), requestId, snippet);

        String errorCode = status.is5xxServerError()
                ? AiAgentErrorCodes.AI_PROVIDER_UNAVAILABLE
                : AiAgentErrorCodes.AI_PROVIDER_BAD_REQUEST;
        throw new LlmProviderException(
                "El proveedor de IA respondio con estado " + status.value() + ".",
                status.value(),
                errorCode);
    }
}
