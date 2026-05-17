package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoIndexingJobRequest;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoIndexingJobResponse;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoRegisterDataSourceRequest;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoRegisterDataSourceResponse;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoSpacesDataSource;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador del puerto {@link IKnowledgeBaseProviderPort} contra la API de
 * DigitalOcean Gradient AI (RF33).
 *
 * <p>Implementa exclusivamente el flujo {@code spaces_data_source}: el
 * binario se subio previamente al bucket Spaces nuestro a traves del
 * {@code IObjectStoragePort} calificado de la KB, y aqui solo registramos
 * el data source apuntando a esa key. La eleccion de Spaces vs file_upload
 * vive en {@code reference_storage_config} y el plan de implementacion.
 */
public class DigitalOceanKnowledgeBaseAdapter implements IKnowledgeBaseProviderPort {

    private static final String KB_DATA_SOURCES_PATH = "/v2/gen-ai/knowledge_bases/%s/data_sources";
    private static final String KB_DATA_SOURCE_PATH = "/v2/gen-ai/knowledge_bases/%s/data_sources/%s";
    private static final String INDEXING_JOBS_PATH = "/v2/gen-ai/indexing_jobs";
    private static final String INDEXING_JOB_PATH = "/v2/gen-ai/indexing_jobs/%s";

    private final DigitalOceanGradientClient client;

    public DigitalOceanKnowledgeBaseAdapter(DigitalOceanGradientClient client) {
        this.client = client;
    }

    @Override
    public RegisteredDataSource registerSpacesDataSource(String knowledgeBaseUuid,
                                                         String bucketName,
                                                         String region,
                                                         String itemPath) {
        DoRegisterDataSourceRequest body = new DoRegisterDataSourceRequest(
                new DoSpacesDataSource(bucketName, itemPath, region)
        );
        DoRegisterDataSourceResponse response = client.post(
                String.format(KB_DATA_SOURCES_PATH, knowledgeBaseUuid),
                body,
                DoRegisterDataSourceResponse.class);
        if (response == null || response.knowledgeBaseDataSource() == null) {
            // No deberia ocurrir si la API respondio 200/201, pero defendemos.
            return new RegisteredDataSource(null, Instant.now());
        }
        DoRegisterDataSourceResponse.DataSourceBody ds = response.knowledgeBaseDataSource();
        return new RegisteredDataSource(ds.uuid(), ds.createdAt() == null ? Instant.now() : ds.createdAt());
    }

    @Override
    public void deleteDataSource(String knowledgeBaseUuid, String dataSourceUuid) {
        client.delete(String.format(KB_DATA_SOURCE_PATH, knowledgeBaseUuid, dataSourceUuid));
    }

    @Override
    public IndexingJobSnapshot startIndexing(String knowledgeBaseUuid, List<String> dataSourceUuids) {
        DoIndexingJobRequest body = new DoIndexingJobRequest(
                knowledgeBaseUuid,
                dataSourceUuids == null || dataSourceUuids.isEmpty() ? null : dataSourceUuids
        );
        DoIndexingJobResponse response = client.post(INDEXING_JOBS_PATH, body, DoIndexingJobResponse.class);
        return toSnapshot(response);
    }

    @Override
    public IndexingJobSnapshot getIndexingJobStatus(String jobId) {
        DoIndexingJobResponse response = client.get(
                String.format(INDEXING_JOB_PATH, jobId),
                DoIndexingJobResponse.class);
        return toSnapshot(response);
    }

    private IndexingJobSnapshot toSnapshot(DoIndexingJobResponse response) {
        if (response == null || response.job() == null) {
            return new IndexingJobSnapshot(null, "UNKNOWN", Instant.now(), null);
        }
        DoIndexingJobResponse.JobBody job = response.job();
        return new IndexingJobSnapshot(
                job.uuid(),
                job.status() == null ? "UNKNOWN" : job.status(),
                job.updatedAt() == null ? Instant.now() : job.updatedAt(),
                job.errorMessage()
        );
    }
}
