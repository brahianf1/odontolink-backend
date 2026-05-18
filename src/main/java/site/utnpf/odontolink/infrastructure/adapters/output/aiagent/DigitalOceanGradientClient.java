package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
 * Cliente HTTP centralizado para llamadas a DigitalOcean Gradient AI.
 *
 * <p>Cada instancia esta cableada con un {@link RestClient} que ya trae el
 * header {@code Authorization: Bearer <token>} del bean al que pertenezca (el
 * management API usa el Personal Access Token; el endpoint del agente usa una
 * access key distinta). De esa forma esta misma clase sirve a los dos
 * subsistemas sin reimplementar HTTP plumbing.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Encapsular llamadas GET/POST/PUT/DELETE contra rutas relativas al
 *       {@code baseUrl} del cliente.</li>
 *   <li>Mapear cualquier 4xx/5xx a {@link LlmProviderException} con el
 *       {@code errorCode} adecuado para que el {@code GlobalExceptionHandler}
 *       traduzca a HTTP 503 con un payload limpio.</li>
 *   <li>Loguear contexto util de la falla: status, request-id, snippet del
 *       body, headers relevantes (en particular {@code WWW-Authenticate} y
 *       {@code content-type}) y una etiqueta {@link #clientLabel} para que
 *       el operador distinga management vs invocation en los logs.</li>
 * </ol>
 *
 * <p>El cap del snippet del body protege contra volcados grandes; el resto
 * del body queda en el response pero no se imprime.
 */
public class DigitalOceanGradientClient {

    private static final Logger log = LoggerFactory.getLogger(DigitalOceanGradientClient.class);
    private static final int MAX_ERROR_BODY_CHARS = 500;

    private final RestClient restClient;
    private final String clientLabel;

    public DigitalOceanGradientClient(RestClient restClient, String clientLabel) {
        this.restClient = restClient;
        this.clientLabel = clientLabel;
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
     * POST a una URL absoluta arbitraria, manteniendo el bearer + manejo de
     * errores de este cliente. Lo usa el adapter de invocacion del agente
     * (chat completions) porque el endpoint del agente vive en un dominio
     * que es descubierto dinamicamente (no esta atado a un baseUrl fijo del
     * cliente).
     *
     * <p>Por que un metodo distinto y no {@link #post(String, Object, Class)}:
     * en el cliente de invocacion el {@code baseUrl} esta intencionalmente
     * vacio para evitar resolves accidentales contra el management API. Un
     * metodo dedicado documenta esta intencion y previene un error de uso.
     */
    public <R> R postAbsolute(String absoluteUrl, Object body, Class<R> responseType) {
        return restClient.post()
                .uri(java.net.URI.create(absoluteUrl))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::translateError)
                .body(responseType);
    }

    /**
     * Traduce cualquier respuesta 4xx/5xx del proveedor en una
     * {@link LlmProviderException} tipada. Es invocado por el contrato
     * {@code onStatus(...)} de {@link RestClient}, asi que debe respetar la
     * firma {@code (HttpRequest, ClientHttpResponse) throws IOException}.
     *
     * <p>El log incluye intencionalmente el {@code WWW-Authenticate} y el
     * {@code content-type} de la respuesta porque son las pistas mas utiles
     * cuando el proveedor rechaza con 401/403 y body vacio: los edge layers
     * suelen agregar {@code WWW-Authenticate} con el motivo (token expirado,
     * scope insuficiente, etc.).
     */
    private void translateError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        byte[] bytes = response.getBody().readAllBytes();
        String body = new String(bytes, StandardCharsets.UTF_8);
        String snippet = body.length() > MAX_ERROR_BODY_CHARS
                ? body.substring(0, MAX_ERROR_BODY_CHARS) + "..."
                : body;

        HttpHeaders headers = response.getHeaders();
        String requestId = headers.getFirst("x-request-id");
        String wwwAuthenticate = headers.getFirst(HttpHeaders.WWW_AUTHENTICATE);
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);

        // Hint operativo: el 401 sin body es el patron clasico cuando se
        // envia el token incorrecto al endpoint del agente. Lo decimos en el
        // log para acelerar el diagnostico en futuras incidencias.
        String hint = (status.value() == 401)
                ? " HINT: 401 sin body suele significar credencial incorrecta para este endpoint. "
                        + "El management API y el endpoint del agente usan tokens distintos."
                : "";

        log.warn("DigitalOcean Gradient [{}] {} {} respondio {} "
                        + "(request-id={}, content-type={}, www-authenticate={}, body-bytes={}). "
                        + "Body: '{}'.{}",
                clientLabel,
                request.getMethod(), request.getURI(), status.value(),
                requestId, contentType, wwwAuthenticate, bytes.length,
                snippet,
                hint);

        String errorCode = status.is5xxServerError()
                ? AiAgentErrorCodes.AI_PROVIDER_UNAVAILABLE
                : AiAgentErrorCodes.AI_PROVIDER_BAD_REQUEST;
        throw new LlmProviderException(
                "El proveedor de IA [" + clientLabel + "] respondio con estado " + status.value() + ".",
                status.value(),
                errorCode);
    }
}
