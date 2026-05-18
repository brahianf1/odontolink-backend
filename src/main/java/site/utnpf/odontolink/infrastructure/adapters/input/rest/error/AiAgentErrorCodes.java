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

    // --- Chatbot institucional (RF29/RF31/RF32/RF34) --------------------

    /**
     * El admin definio accessMode=DISABLED. El chatbot rechaza requests
     * (403) hasta que se vuelva a habilitar.
     */
    public static final String AI_AGENT_DISABLED = "AI_AGENT_DISABLED";

    /**
     * El admin definio accessMode=PRIVATE pero el caller autenticado no
     * tiene un rol incluido en {@code allowedRoles}. 403.
     */
    public static final String AI_AGENT_ACCESS_DENIED = "AI_AGENT_ACCESS_DENIED";

    /**
     * El admin definio accessMode=PRIVATE y el caller llego sin autenticarse.
     * 401: el FE debe llevar al login antes de reintentar.
     */
    public static final String AI_AGENT_ANONYMOUS_FORBIDDEN = "AI_AGENT_ANONYMOUS_FORBIDDEN";

    /**
     * Se excedio el cap por hora del chatbot (anonimo por IP o autenticado
     * por usuario). 429 con header {@code Retry-After}.
     */
    public static final String AI_RATE_LIMIT_EXCEEDED = "AI_RATE_LIMIT_EXCEEDED";

    /**
     * El mensaje excede el cap de 2000 caracteres del DTO. 422.
     */
    public static final String AI_MESSAGE_TOO_LONG = "AI_MESSAGE_TOO_LONG";

    /**
     * No se puede resolver la URL de invocacion del agente: ni la ENV esta
     * seteada ni el cache local existe ni el descubrimiento via management
     * API devolvio un deployment.url. 503.
     */
    public static final String AI_AGENT_INVOCATION_URL_UNAVAILABLE = "AI_AGENT_INVOCATION_URL_UNAVAILABLE";
}
