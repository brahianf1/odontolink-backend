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
import site.utnpf.odontolink.application.port.in.IChatbotInteractionUseCase;
import site.utnpf.odontolink.application.port.in.IEmergencyKeywordAdminUseCase;
import site.utnpf.odontolink.application.port.in.IAgentPolicyRuleAdminUseCase;
import site.utnpf.odontolink.application.port.in.IKnowledgeBaseAdminUseCase;
import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.application.service.AiAgentConfigurationService;
import site.utnpf.odontolink.application.service.AiAgentVersioningService;
import site.utnpf.odontolink.application.service.AiGovernancePolicyService;
import site.utnpf.odontolink.application.service.ChatbotInteractionService;
import site.utnpf.odontolink.application.service.EmergencyKeywordAdminService;
import site.utnpf.odontolink.application.service.AgentPolicyRuleAdminService;
import site.utnpf.odontolink.application.service.KnowledgeBaseAdminService;
import site.utnpf.odontolink.application.service.security.EmergencyDetector;
import site.utnpf.odontolink.application.service.security.PiiSanitizer;
import site.utnpf.odontolink.application.service.support.SingletonRowBootstrap;
import site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig;
import site.utnpf.odontolink.domain.repository.AiAdminAuditEventRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationVersionRepository;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
import site.utnpf.odontolink.domain.repository.ChatbotMessageRepository;
import site.utnpf.odontolink.domain.repository.ChatbotSessionRepository;
import site.utnpf.odontolink.domain.repository.EmergencyKeywordRepository;
import site.utnpf.odontolink.domain.repository.AgentPolicyRuleRepository;
import site.utnpf.odontolink.domain.repository.KnowledgeBaseDocumentRepository;
import site.utnpf.odontolink.domain.service.ConfidenceCalculator;
import site.utnpf.odontolink.domain.service.RefusalDetector;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanAgentInvokerAdapter;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanAgentPlatformProperties;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanGradientClient;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanKnowledgeBaseAdapter;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanLlmAgentAdapter;
import site.utnpf.odontolink.infrastructure.adapters.output.storage.S3CompatibleObjectStorageAdapter;
import site.utnpf.odontolink.infrastructure.config.confidence.ConfidenceCalculatorProperties;
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
@EnableConfigurationProperties({
        DigitalOceanAgentPlatformProperties.class,
        ConfidenceCalculatorProperties.class
})
public class AiAgentBeanConfiguration {

    /**
     * RestClient para el <strong>management API</strong> de DigitalOcean
     * Gradient ({@code api.digitalocean.com/v2/gen-ai/*}). Usa el Personal
     * Access Token (PAT) como bearer; este token autoriza operaciones
     * administrativas: leer/actualizar agentes, manejar la Knowledge Base,
     * disparar indexing jobs.
     */
    @Bean(name = "doGradientManagementRestClient")
    public RestClient doGradientManagementRestClient(DigitalOceanAgentPlatformProperties props,
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

    /**
     * RestClient para la <strong>invocacion del agente</strong> (chat
     * completions) en {@code <id>.agents.do-ai.run}. Usa la access key del
     * endpoint del agente, NO el PAT — cada agente genera su propia key en
     * el dashboard "Endpoint Keys". Confundir tokens devuelve 401 sin body
     * (el bug original de produccion).
     *
     * <p>Sin baseUrl: la URL del agente se pasa absoluta en cada llamada
     * ({@code postAbsolute}) porque puede ser descubierta dinamicamente o
     * cambiar entre deploys.
     */
    @Bean(name = "doGradientInvocationRestClient")
    public RestClient doGradientInvocationRestClient(DigitalOceanAgentPlatformProperties props,
                                                     RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());

        return builder
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Bearer " + props.getAgentInvocationAccessKey())
                .build();
    }

    /**
     * RestClient identico al de invocacion pero con un read timeout
     * <strong>mas corto</strong> ({@code probeReadTimeoutMs}, default 5s vs
     * 20s del normal). Lo usa el adapter para el {@code probe()} del
     * health-check: si el agente esta caido queremos que el endpoint
     * {@code /health} responda en pocos segundos, no que tarde 20s mientras
     * el RestClient bloquea esperando bytes.
     */
    @Bean(name = "doGradientInvocationProbeRestClient")
    public RestClient doGradientInvocationProbeRestClient(DigitalOceanAgentPlatformProperties props,
                                                          RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getProbeReadTimeoutMs());

        return builder
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Bearer " + props.getAgentInvocationAccessKey())
                .build();
    }

    /**
     * Cliente de management. Reemplazo del bean unico que existia antes; el
     * nombre del bean lo mantenemos calificado para no chocar con el de
     * invocacion.
     */
    @Bean(name = "doGradientManagementClient")
    public DigitalOceanGradientClient doGradientManagementClient(
            @Qualifier("doGradientManagementRestClient") RestClient restClient) {
        return new DigitalOceanGradientClient(restClient, "management");
    }

    /** Cliente dedicado al endpoint del agente para invocaciones normales. */
    @Bean(name = "doGradientInvocationClient")
    public DigitalOceanGradientClient doGradientInvocationClient(
            @Qualifier("doGradientInvocationRestClient") RestClient restClient) {
        return new DigitalOceanGradientClient(restClient, "invocation");
    }

    /** Cliente para el probe del health-check (timeout corto). */
    @Bean(name = "doGradientInvocationProbeClient")
    public DigitalOceanGradientClient doGradientInvocationProbeClient(
            @Qualifier("doGradientInvocationProbeRestClient") RestClient restClient) {
        return new DigitalOceanGradientClient(restClient, "invocation-probe");
    }

    @Bean
    public ILlmAgentProviderPort llmAgentProviderPort(
            @Qualifier("doGradientManagementClient") DigitalOceanGradientClient client) {
        return new DigitalOceanLlmAgentAdapter(client);
    }

    @Bean
    public IKnowledgeBaseProviderPort knowledgeBaseProviderPort(
            @Qualifier("doGradientManagementClient") DigitalOceanGradientClient client) {
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
            AgentPolicyRuleRepository policyRuleRepository,
            site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository providerGuardrailRepository,
            AiGovernancePolicyRepository policyRepository,
            AiAgentConfigurationVersionRepository versionRepository,
            AiAdminAuditEventRepository auditRepository,
            KnowledgeBaseDocumentRepository kbDocumentRepository,
            ILlmAgentProviderPort llmProvider,
            ILlmAgentInvokerPort invokerPort,
            AuthenticationFacade authFacade,
            SingletonRowBootstrap singletonBootstrap,
            DigitalOceanAgentPlatformProperties props) {
        return new AiAgentConfigurationService(
                configRepository,
                policyRuleRepository,
                providerGuardrailRepository,
                policyRepository,
                versionRepository,
                auditRepository,
                kbDocumentRepository,
                llmProvider,
                invokerPort,
                authFacade,
                singletonBootstrap,
                props.getAgentUuid(),
                props.getAgentInvocationUrl()
        );
    }

    @Bean
    public IAgentPolicyRuleAdminUseCase agentPolicyRuleAdminUseCase(
            AgentPolicyRuleRepository policyRuleRepository,
            AiAgentConfigurationRepository configRepository) {
        return new AgentPolicyRuleAdminService(policyRuleRepository, configRepository);
    }

    @Bean
    public site.utnpf.odontolink.application.port.in.IProviderGuardrailAdminUseCase providerGuardrailAdminUseCase(
            site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository guardrailRepository,
            AiAgentConfigurationRepository configRepository,
            ILlmAgentProviderPort llmProvider,
            DigitalOceanAgentPlatformProperties props) {
        return new site.utnpf.odontolink.application.service.ProviderGuardrailAdminService(
                guardrailRepository, configRepository, llmProvider, props.getAgentUuid());
    }

    @Bean
    public IAiGovernancePolicyUseCase aiGovernancePolicyUseCase(
            AiGovernancePolicyRepository policyRepository,
            AiAdminAuditEventRepository auditRepository,
            AuthenticationFacade authFacade,
            SingletonRowBootstrap singletonBootstrap) {
        return new AiGovernancePolicyService(policyRepository, auditRepository, authFacade, singletonBootstrap);
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

    // -- Beans del chatbot institucional (RF29/RF31/RF32/RF34) -----------

    @Bean
    public PiiSanitizer piiSanitizer() {
        return new PiiSanitizer();
    }

    @Bean
    public EmergencyDetector emergencyDetector() {
        return new EmergencyDetector();
    }

    /**
     * Adapta las properties Spring a un record de dominio inmutable. El
     * calculador y el detector dependen solo del dominio (sin Spring), lo
     * que los hace testeables sin contexto y reutilizables fuera del modulo.
     */
    @Bean
    public ConfidenceCalculatorConfig confidenceCalculatorConfig(ConfidenceCalculatorProperties props) {
        return props.toDomainConfig();
    }

    @Bean
    public RefusalDetector refusalDetector(ConfidenceCalculatorConfig config) {
        return new RefusalDetector(config);
    }

    @Bean
    public ConfidenceCalculator confidenceCalculator(ConfidenceCalculatorConfig config,
                                                     RefusalDetector refusalDetector) {
        return new ConfidenceCalculator(config, refusalDetector);
    }

    /**
     * Adapter de invocacion del agente. Reutiliza el {@link DigitalOceanGradientClient}
     * dedicado al endpoint del agente, NO el del management API: el agente
     * usa una access key propia que se genera en su seccion "Endpoint Keys".
     * Resilience4j envuelve las invocaciones gracias a sus anotaciones.
     */
    @Bean
    public ILlmAgentInvokerPort llmAgentInvokerPort(
            @Qualifier("doGradientInvocationClient") DigitalOceanGradientClient invocationClient,
            @Qualifier("doGradientInvocationProbeClient") DigitalOceanGradientClient probeClient,
            DigitalOceanAgentPlatformProperties props) {
        return new DigitalOceanAgentInvokerAdapter(invocationClient, probeClient, props.getInvocationModel());
    }

    @Bean
    public IChatbotInteractionUseCase chatbotInteractionUseCase(
            AiAgentConfigurationRepository configRepository,
            ChatbotSessionRepository sessionRepository,
            ChatbotMessageRepository messageRepository,
            EmergencyKeywordRepository emergencyKeywordRepository,
            PiiSanitizer piiSanitizer,
            EmergencyDetector emergencyDetector,
            ILlmAgentInvokerPort invokerPort,
            ILlmAgentProviderPort providerPort,
            ConfidenceCalculator confidenceCalculator,
            DigitalOceanAgentPlatformProperties props) {
        // Nota: el GuardrailRepository se quito a proposito. Los guardrails se
        // componen al system prompt SOLO en el flujo de publish() (lo hace
        // AiAgentConfigurationService); DigitalOcean Gradient los aplica
        // server-side en cada invocacion porque vienen del agent.instruction
        // ya sincronizado. Inyectarlos aqui era duplicacion y, peor, hacia
        // que el wire enviara role=system que DO rechaza con 400 cuando se
        // invoca con ?agent=true.
        return new ChatbotInteractionService(
                configRepository,
                sessionRepository,
                messageRepository,
                emergencyKeywordRepository,
                piiSanitizer,
                emergencyDetector,
                invokerPort,
                providerPort,
                confidenceCalculator,
                props.getAgentInvocationUrl(),
                props.getAgentUuid()
        );
    }

    @Bean
    public IEmergencyKeywordAdminUseCase emergencyKeywordAdminUseCase(
            EmergencyKeywordRepository repository) {
        return new EmergencyKeywordAdminService(repository);
    }
}
