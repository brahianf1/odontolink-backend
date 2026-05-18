package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del modulo de IA, vinculadas a {@code odontolink.ai-agent.*}.
 *
 * <p>Se mantienen separadas en una clase tipada para que la configuracion
 * sea descubrible (IDE-friendly) y para evitar dispersar {@code @Value} en
 * adapters y servicios.
 *
 * <p>Subobjeto {@code storage}: representa el bucket S3-compatible dedicado
 * a la Knowledge Base. Es OBLIGATORIAMENTE un bucket DigitalOcean Spaces
 * porque la API de Gradient solo puede leer de Spaces o AWS S3 nativo via
 * {@code spaces_data_source} / {@code aws_data_source}. R2/MinIO/B2 no son
 * compatibles para indexacion remota.
 */
@ConfigurationProperties(prefix = "odontolink.ai-agent")
public class DigitalOceanAgentPlatformProperties {

    /**
     * Base URL del proveedor. Configurable para tests/staging con un servidor
     * mock; en produccion siempre es {@code https://api.digitalocean.com}.
     */
    private String endpointBaseUrl = "https://api.digitalocean.com";

    /** Personal Access Token de DigitalOcean con scope de Gradient AI. */
    private String accessToken = "";

    /** UUID del agente pre-provisionado en el dashboard de DO. */
    private String agentUuid = "";

    /** UUID de la knowledge base pre-provisionada en el dashboard de DO. */
    private String knowledgeBaseUuid = "";

    /** Timeout de conexion para llamadas a DO, en milisegundos. */
    private int connectTimeoutMs = 5000;

    /** Timeout de lectura para llamadas a DO, en milisegundos. */
    private int readTimeoutMs = 20000;

    /** Tamanio maximo permitido para archivos subidos a la KB, en bytes. */
    private long maxUploadBytes = 10_485_760L;

    /**
     * URL de invocacion del agente (chat completions). Si se setea, gana sobre
     * el cache local en BD. Si esta vacia, el chatbot intenta descubrirla via
     * management API ({@code GET /v2/gen-ai/agents/{uuid}}) y cachearla en
     * BD para sucesivas llamadas. (RF29).
     */
    private String agentInvocationUrl = "";

    /** Subgrupo de propiedades del bucket Spaces dedicado a la KB. */
    private Storage storage = new Storage();

    public String getEndpointBaseUrl() {
        return endpointBaseUrl;
    }

    public void setEndpointBaseUrl(String endpointBaseUrl) {
        this.endpointBaseUrl = endpointBaseUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    public void setAgentUuid(String agentUuid) {
        this.agentUuid = agentUuid;
    }

    public String getKnowledgeBaseUuid() {
        return knowledgeBaseUuid;
    }

    public void setKnowledgeBaseUuid(String knowledgeBaseUuid) {
        this.knowledgeBaseUuid = knowledgeBaseUuid;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public String getAgentInvocationUrl() {
        return agentInvocationUrl;
    }

    public void setAgentInvocationUrl(String agentInvocationUrl) {
        this.agentInvocationUrl = agentInvocationUrl;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * Configuracion del bucket S3-compatible (Spaces) dedicado a la KB.
     * Vive separado del bucket de fotos de perfil ({@code storage.s3.*}) para
     * permitir distintos buckets, regiones y credenciales sin afectar al
     * resto del sistema.
     */
    public static class Storage {

        private String endpoint = "";
        private String region = "";
        private String bucket = "";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private String publicBaseUrl = "";
        private boolean pathStyle = false;
        private String keyPrefix = "ai-knowledge-base";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public boolean isPathStyle() {
            return pathStyle;
        }

        public void setPathStyle(boolean pathStyle) {
            this.pathStyle = pathStyle;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
