package site.utnpf.odontolink.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import site.utnpf.odontolink.application.port.in.IAiAgentConfigurationUseCase;
import site.utnpf.odontolink.application.port.in.IAiAgentVersioningUseCase;
import site.utnpf.odontolink.application.port.in.IAiGovernancePolicyUseCase;
import site.utnpf.odontolink.application.port.in.IGuardrailAdminUseCase;
import site.utnpf.odontolink.application.port.in.IKnowledgeBaseAdminUseCase;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.application.service.AiAgentConfigurationService;
import site.utnpf.odontolink.application.service.AiAgentVersioningService;
import site.utnpf.odontolink.application.service.AiGovernancePolicyService;
import site.utnpf.odontolink.application.service.GuardrailAdminService;
import site.utnpf.odontolink.application.service.KnowledgeBaseAdminService;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
import site.utnpf.odontolink.domain.repository.GuardrailRepository;
import site.utnpf.odontolink.domain.repository.KnowledgeBaseDocumentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanAgentPlatformProperties;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanGradientClient;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanKnowledgeBaseAdapter;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanLlmAgentAdapter;
import site.utnpf.odontolink.infrastructure.adapters.output.storage.S3CompatibleObjectStorageAdapter;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Configuracion de beans del modulo de administracion del agente IA
 * (RF31, RF32, RF33).
 *
 * <p>Mantenida separada del {@code BeanConfiguration} global para acotar
 * el blast radius: el modulo IA es independiente del resto y un futuro
 * reemplazo del proveedor toca un solo archivo.
 */
@Configuration
@EnableConfigurationProperties(DigitalOceanAgentPlatformProperties.class)
public class AiAgentBeanConfiguration {

    @Bean
    public RestClient digitalOceanGradientRestClient(DigitalOceanAgentPlatformProperties props,
                                                     RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());

        return builder
                .baseUrl(props.getEndpointBaseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getAccessToken())
                .build();
    }

    @Bean
    public DigitalOceanGradientClient digitalOceanGradientClient(
            RestClient digitalOceanGradientRestClient) {
        return new DigitalOceanGradientClient(digitalOceanGradientRestClient);
    }

    @Bean
    public ILlmAgentProviderPort llmAgentProviderPort(DigitalOceanGradientClient client) {
        return new DigitalOceanLlmAgentAdapter(client);
    }

    @Bean
    public IKnowledgeBaseProviderPort knowledgeBaseProviderPort(DigitalOceanGradientClient client) {
        return new DigitalOceanKnowledgeBaseAdapter(client);
    }

    /**
     * Cliente S3 dedicado al bucket Spaces de la Knowledge Base. Aislado
     * del cliente de fotos de perfil para soportar buckets, regiones y
     * credenciales distintas.
     */
    @Bean(name = "aiKbS3Client", destroyMethod = "close")
    public S3Client aiKbS3Client(DigitalOceanAgentPlatformProperties props) {
        DigitalOceanAgentPlatformProperties.Storage storage = props.getStorage();
        String endpoint = (storage.getEndpoint() == null || storage.getEndpoint().isBlank())
                ? "https://invalid-ai-kb-endpoint.localhost"
                : storage.getEndpoint();
        String region = (storage.getRegion() == null || storage.getRegion().isBlank())
                ? "auto"
                : storage.getRegion();
        String accessKey = (storage.getAccessKeyId() == null || storage.getAccessKeyId().isBlank())
                ? "unset"
                : storage.getAccessKeyId();
        String secretKey = (storage.getSecretAccessKey() == null || storage.getSecretAccessKey().isBlank())
                ? "unset"
                : storage.getSecretAccessKey();

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(storage.isPathStyle())
                .build();
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClient(UrlConnectionHttpClient.create())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .serviceConfiguration(s3Config)
                .build();
    }

    @Bean
    @Qualifier("aiKbObjectStorage")
    public IObjectStoragePort aiKbObjectStorage(@Qualifier("aiKbS3Client") S3Client aiKbS3Client,
                                                DigitalOceanAgentPlatformProperties props) {
        return new S3CompatibleObjectStorageAdapter(
                aiKbS3Client,
                props.getStorage().getBucket(),
                props.getStorage().getPublicBaseUrl()
        );
    }

    @Bean
    public IAiAgentConfigurationUseCase aiAgentConfigurationUseCase(
            AiAgentConfigurationRepository configRepository,
            GuardrailRepository guardrailRepository,
            AiGovernancePolicyRepository policyRepository,
            AiAgentConfigurationVersionRepository versionRepository,
            AiAdminAuditEventRepository auditRepository,
            KnowledgeBaseDocumentRepository kbDocumentRepository,
            ILlmAgentProviderPort llmProvider,
            AuthenticationFacade authFacade,
            DigitalOceanAgentPlatformProperties props) {
        return new AiAgentConfigurationService(
                configRepository,
                guardrailRepository,
                policyRepository,
                versionRepository,
                auditRepository,
                kbDocumentRepository,
                llmProvider,
                authFacade,
                props.getAgentUuid()
        );
    }

    @Bean
    public IGuardrailAdminUseCase guardrailAdminUseCase(
            GuardrailRepository guardrailRepository,
            AiAgentConfigurationRepository configRepository) {
        return new GuardrailAdminService(guardrailRepository, configRepository);
    }

    @Bean
    public IAiGovernancePolicyUseCase aiGovernancePolicyUseCase(
            AiGovernancePolicyRepository policyRepository,
            AiAdminAuditEventRepository auditRepository,
            AuthenticationFacade authFacade) {
        return new AiGovernancePolicyService(policyRepository, auditRepository, authFacade);
    }

    @Bean
    public IAiAgentVersioningUseCase aiAgentVersioningUseCase(
            AiAgentConfigurationVersionRepository versionRepository,
            AiAgentConfigurationRepository configRepository,
            AiAdminAuditEventRepository auditRepository,
            ILlmAgentProviderPort llmProvider,
            AuthenticationFacade authFacade,
            DigitalOceanAgentPlatformProperties props) {
        return new AiAgentVersioningService(
                versionRepository,
                configRepository,
                auditRepository,
                llmProvider,
                authFacade,
                props.getAgentUuid()
        );
    }

    @Bean
    public IKnowledgeBaseAdminUseCase knowledgeBaseAdminUseCase(
            KnowledgeBaseDocumentRepository documentRepository,
            IKnowledgeBaseProviderPort kbProvider,
            @Qualifier("aiKbObjectStorage") IObjectStoragePort aiKbStorage,
            DigitalOceanAgentPlatformProperties props) {
        return new KnowledgeBaseAdminService(
                documentRepository,
                kbProvider,
                aiKbStorage,
                props.getKnowledgeBaseUuid(),
                props.getStorage().getBucket(),
                props.getStorage().getRegion(),
                props.getStorage().getKeyPrefix(),
                props.getMaxUploadBytes()
        );
    }
}
