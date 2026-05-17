package site.utnpf.odontolink.infrastructure.adapters.output.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.application.port.out.StorageException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Implementacion del puerto {@link IObjectStoragePort} contra cualquier
 * backend S3-compatible. Probado con Cloudflare R2 y DigitalOcean Spaces, y
 * deberia funcionar sin cambios contra AWS S3, MinIO, Backblaze B2.
 *
 * <p>Diseno: el adapter es una clase reutilizable que recibe el
 * {@link S3Client} y el bucket por constructor, en lugar de auto-cablearse
 * con {@code @Value}. Esto permite registrar multiples beans con buckets
 * distintos (p. ej. uno para fotos de perfil, otro para la Knowledge Base
 * del modulo IA) sin duplicar codigo. Las {@code BeanConfiguration} cablean
 * las instancias concretas con el qualifier adecuado.
 *
 * <p>El ciclo de vida del {@link S3Client} se gestiona desde la
 * configuracion de beans (atributo {@code destroyMethod = "close"} en la
 * declaracion del bean S3Client): esta clase NO cierra el cliente, porque
 * podria ser compartido entre adapters.
 */
public class S3CompatibleObjectStorageAdapter implements IObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3CompatibleObjectStorageAdapter.class);

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3CompatibleObjectStorageAdapter(S3Client s3Client, String bucket, String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        requireConfigured();
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
        requireConfigured();
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
    public IObjectStoragePort.DownloadedObject download(String key) {
        requireConfigured();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return new IObjectStoragePort.DownloadedObject(
                    response.asByteArray(), response.response().contentType());
        } catch (NoSuchKeyException ex) {
            throw new StorageException("Objeto '" + key + "' no encontrado en el storage.", ex);
        } catch (S3Exception ex) {
            throw new StorageException("Falla al descargar el objeto '" + key + "' del storage.", ex);
        }
    }

    @Override
    public String buildPublicUrl(String key) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new StorageException(
                    "publicBaseUrl no configurado para este bucket: imposible exponer URL publica.");
        }
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String trimmedKey = key.startsWith("/") ? key.substring(1) : key;
        return base + "/" + trimmedKey;
    }

    private void requireConfigured() {
        if (s3Client == null) {
            throw new StorageException(
                    "S3 client no inicializado. Verifique las variables S3 del entorno correspondiente.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new StorageException(
                    "Bucket no configurado para este adapter de storage.");
        }
        // Log silenciado por defecto; util solo cuando se diagnosticando
        // problemas de configuracion en un ambiente nuevo.
        log.trace("S3 adapter operating on bucket={}", bucket);
    }
}
