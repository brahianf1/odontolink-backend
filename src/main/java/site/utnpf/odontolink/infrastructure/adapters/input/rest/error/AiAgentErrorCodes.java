package site.utnpf.odontolink.infrastructure.adapters.input.rest.error;

/**
 * Catalogo de {@code errorCode}s estables del subsistema de administracion
 * del agente IA (RF31, RF32, RF33).
 *
 * <p>Estos codigos viajan en el body de las respuestas 4xx/5xx para que el
 * frontend pueda ramificar UX sin parsear mensajes humanos. Convencion:
 * prefijo {@code AI_} en SCREAMING_SNAKE.
 */
public final class AiAgentErrorCodes {

    private AiAgentErrorCodes() {
    }

    // --- 503 Service Unavailable -----------------------------------------

    /**
     * El proveedor de IA externo respondio con 5xx o la comunicacion fallo
     * (timeouts, DNS, TLS). El frontend debe sugerir reintentar y, si
     * persiste, recurrir al endpoint de resync para forzar reconciliacion.
     */
    public static final String AI_PROVIDER_UNAVAILABLE = "AI_PROVIDER_UNAVAILABLE";

    /**
     * El proveedor de IA respondio con 4xx (request mal formado, credencial
     * invalida, UUID inexistente). Distinto del unavailable porque indica
     * problema de configuracion, no de disponibilidad.
     */
    public static final String AI_PROVIDER_BAD_REQUEST = "AI_PROVIDER_BAD_REQUEST";

    // --- 422 Unprocessable Entity ---------------------------------------

    /**
     * Reglas de validacion del agregado {@code AiAgentConfiguration} no
     * cumplidas (rangos de temperatura/topP, maxTokens, etc.). Permite al
     * frontend resaltar el campo invalido sin parsear el mensaje.
     */
    public static final String AI_AGENT_CONFIG_INVALID = "AI_AGENT_CONFIG_INVALID";

    /**
     * El archivo subido excede el tope configurado para la KB.
     */
    public static final String AI_KB_FILE_TOO_LARGE = "AI_KB_FILE_TOO_LARGE";

    /**
     * El archivo subido esta vacio.
     */
    public static final String AI_KB_FILE_EMPTY = "AI_KB_FILE_EMPTY";

    /**
     * El MIME type del archivo no pertenece a la whitelist de tipos soportados.
     */
    public static final String AI_KB_UNSUPPORTED_TYPE = "AI_KB_UNSUPPORTED_TYPE";

    /**
     * El indexing job termino en estado fallido en el proveedor. El frontend
     * puede ofrecer reintentar via POST /reindex.
     */
    public static final String AI_KB_INDEXING_FAILED = "AI_KB_INDEXING_FAILED";

    /**
     * Se intento publicar o revertir un agente cuya configuracion todavia no
     * fue creada (lifecycle virtual UNCONFIGURED). El frontend debe redirigir
     * al wizard de primera carga.
     */
    public static final String AI_AGENT_NOT_CONFIGURED = "AI_AGENT_NOT_CONFIGURED";

    /**
     * Se intento {@code revert-to-draft} sobre una configuracion que no esta
     * publicada. Volver a DRAFT desde DRAFT/UNCONFIGURED es un no-op
     * encubierto: lo rechazamos para que el frontend no pinte una falsa
     * transicion.
     */
    public static final String AI_AGENT_NOT_PUBLISHED = "AI_AGENT_NOT_PUBLISHED";

    // --- 404 Not Found --------------------------------------------------

    public static final String AI_KB_DOCUMENT_NOT_FOUND = "AI_KB_DOCUMENT_NOT_FOUND";
}
