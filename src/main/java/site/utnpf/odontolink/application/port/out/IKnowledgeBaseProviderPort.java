package site.utnpf.odontolink.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida para la administracion de la Knowledge Base remota (RF33).
 *
 * <p>El acoplamiento al proveedor concreto (DigitalOcean Gradient) vive
 * exclusivamente en el adapter. La capa de aplicacion conoce solo este
 * contrato.
 *
 * <p>La operacion de upload del binario NO esta aqui: el binario se sube al
 * bucket S3-compatible mediante {@link IObjectStoragePort}. Lo que este
 * puerto registra es un data source que apunta a ese objeto, de modo que el
 * proveedor pueda leerlo durante la indexacion.
 */
public interface IKnowledgeBaseProviderPort {

    /**
     * Resultado del registro de un data source: UUID emitido por el proveedor.
     */
    record RegisteredDataSource(
            String providerDataSourceId,
            Instant createdAt) {
    }

    /**
     * Estado de un indexing job. {@code status} se serializa como string
     * porque el set de estados puede crecer; el caller deduce si esta en
     * progreso, terminado o fallido segun el valor reportado.
     */
    record IndexingJobSnapshot(
            String jobId,
            String status,
            Instant updatedAt,
            String errorMessage) {
    }

    /**
     * Registra un objeto subido al bucket Spaces como data source de la KB.
     * El proveedor leera el objeto mediante el binding {@code spaces_data_source}
     * y lo indexara en el siguiente run.
     *
     * @param knowledgeBaseUuid UUID de la KB en el proveedor.
     * @param bucketName        nombre del bucket Spaces donde se subio el archivo.
     * @param region            region del bucket (ej. {@code "sfo3"}).
     * @param itemPath          clave completa del objeto dentro del bucket.
     */
    RegisteredDataSource registerSpacesDataSource(String knowledgeBaseUuid,
                                                  String bucketName,
                                                  String region,
                                                  String itemPath);

    void deleteDataSource(String knowledgeBaseUuid, String dataSourceUuid);

    /**
     * Dispara un indexing job. Si {@code dataSourceUuids} viene vacio o null,
     * el proveedor indexa todo el contenido pendiente de la KB.
     */
    IndexingJobSnapshot startIndexing(String knowledgeBaseUuid, List<String> dataSourceUuids);

    IndexingJobSnapshot getIndexingJobStatus(String jobId);
}
