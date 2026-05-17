package site.utnpf.odontolink.application.port.out;

/**
 * Puerto de salida para almacenamiento de objetos (binarios).
 *
 * <p>Modela una interfaz minima compatible con cualquier object storage
 * S3-compliant: AWS S3, Cloudflare R2, MinIO, Backblaze B2, DigitalOcean
 * Spaces, etc. La capa de aplicacion solo conoce este contrato; el cambio
 * de proveedor se hace via configuracion (endpoint + credenciales) y, en
 * caso extremo, sustituyendo el adaptador concreto sin tocar use cases.
 */
public interface IObjectStoragePort {

    /**
     * Sube un objeto al storage.
     *
     * @param key         clave del objeto dentro del bucket (p.ej.
     *                    {@code "profile-pictures/15/abc.jpg"}). Debe ser
     *                    unico para evitar pisar contenido ajeno.
     * @param content     contenido binario completo a subir.
     * @param contentType MIME type oficial del contenido (afecta el header
     *                    {@code Content-Type} que servira el storage cuando
     *                    el objeto se descargue).
     * @return URL publica del objeto recien subido (ver
     *         {@link #buildPublicUrl(String)}).
     * @throws StorageException si la operacion falla por errores de red,
     *                          credenciales o configuracion del bucket.
     */
    String upload(String key, byte[] content, String contentType);

    /**
     * Borra un objeto del storage. Idempotente: si el objeto no existe la
     * llamada no produce error.
     */
    void delete(String key);

    /**
     * Resultado de una descarga: el binario y el MIME type que el storage
     * tiene registrado para ese objeto. El MIME viaja por separado para que
     * el caller pueda construir respuestas HTTP fieles sin re-inspeccionar
     * los bytes.
     */
    record DownloadedObject(byte[] content, String contentType) {
    }

    /**
     * Descarga un objeto del storage.
     *
     * @throws StorageException si el objeto no existe o la operacion falla
     *                          por errores de red / credenciales.
     */
    DownloadedObject download(String key);

    /**
     * Construye la URL publica para una clave dada combinando la base
     * publica configurada del bucket con la clave del objeto.
     *
     * <p>Util para reconstruir URLs sin pegar al storage cuando ya tenemos
     * la key en BD.
     */
    String buildPublicUrl(String key);
}
