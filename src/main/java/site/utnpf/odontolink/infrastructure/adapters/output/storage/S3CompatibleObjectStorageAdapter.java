package site.utnpf.odontolink.infrastructure.adapters.output.storage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.application.port.out.StorageException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

/**
 * Implementacion del puerto {@link IObjectStoragePort} contra cualquier
 * backend S3-compatible. Probado con Cloudflare R2 y deberia funcionar sin
 * cambios contra AWS S3, MinIO, Backblaze B2 y DigitalOcean Spaces.
 *
 * <p>Toda la configuracion viaja por variables de entorno:
 * <pre>
 *   STORAGE_S3_ENDPOINT          (ej. https://&lt;account-id&gt;.r2.cloudflarestorage.com)
 *   STORAGE_S3_REGION            (R2: "auto"; AWS: ej. "us-east-1")
 *   STORAGE_S3_BUCKET            nombre del bucket
 *   STORAGE_S3_ACCESS_KEY_ID
 *   STORAGE_S3_SECRET_ACCESS_KEY
 *   STORAGE_S3_PUBLIC_BASE_URL   URL publica desde la que se sirven los objetos
 *                                (R2: el subdominio pub-XXX.r2.dev o un custom domain)
 *   STORAGE_S3_PATH_STYLE        true para forzar path-style; default false (virtual-hosted)
 * </pre>
 *
 * <p>Mantener todas las opciones del lado del entorno (no en codigo) permite
 * mover entre proveedores sin tocar un solo archivo Java.
 */
@Component
public class S3CompatibleObjectStorageAdapter implements IObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3CompatibleObjectStorageAdapter.class);

    @Value("${storage.s3.endpoint}")
    private String endpoint;

    @Value("${storage.s3.region}")
    private String region;

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Value("${storage.s3.access-key-id}")
    private String accessKeyId;

    @Value("${storage.s3.secret-access-key}")
    private String secretAccessKey;

    @Value("${storage.s3.public-base-url}")
    private String publicBaseUrl;

    @Value("${storage.s3.path-style:false}")
    private boolean pathStyle;

    private S3Client s3Client;

    @PostConstruct
    void init() {
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("storage.s3.endpoint no configurado: el adaptador de storage emitira errores al ser usado.");
            return;
        }

        try {
            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyle)
                    .build();

            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .httpClient(UrlConnectionHttpClient.create())
                    // R2 y otros backends S3-compatibles aun no soportan los
                    // nuevos checksums por defecto del SDK 2.30+. Forzamos
                    // WHEN_REQUIRED en ambos sentidos para mantener compat.
                    .requestChecksumCalculation(software.amazon.awssdk.core.checksums.RequestChecksumCalculation.WHEN_REQUIRED)
                    .responseChecksumValidation(software.amazon.awssdk.core.checksums.ResponseChecksumValidation.WHEN_REQUIRED)
                    .serviceConfiguration(s3Config)
                    .build();

            log.info("S3 client initialized: endpoint={}, region={}, bucket={}, pathStyle={}",
                    endpoint, region, bucket, pathStyle);
        } catch (Exception ex) {
            log.error("Falla al inicializar S3Client; los uploads/deletes lanzaran StorageException.", ex);
        }
    }

    @PreDestroy
    void close() {
        if (s3Client != null) {
            try {
                s3Client.close();
            } catch (Exception ex) {
                log.warn("Falla al cerrar S3Client", ex);
            }
        }
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        requireClient();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            return buildPublicUrl(key);
        } catch (S3Exception ex) {
            throw new StorageException("Falla al subir el objeto '" + key + "' al storage.", ex);
        }
    }

    @Override
    public void delete(String key) {
        requireClient();
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (NoSuchKeyException ignored) {
            // Idempotente: borrar lo que no existe no es un error.
        } catch (S3Exception ex) {
            throw new StorageException("Falla al borrar el objeto '" + key + "' del storage.", ex);
        }
    }

    @Override
    public String buildPublicUrl(String key) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new StorageException(
                    "storage.s3.public-base-url no esta configurado: imposible exponer URL publica.");
        }
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String trimmedKey = key.startsWith("/") ? key.substring(1) : key;
        return base + "/" + trimmedKey;
    }

    private void requireClient() {
        if (s3Client == null) {
            throw new StorageException(
                    "S3 client no inicializado. Verifique las variables STORAGE_S3_* en el entorno.");
        }
    }
}
