package site.utnpf.odontolink.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.domain.model.InstitutionalSettings;
import site.utnpf.odontolink.domain.repository.AiGovernancePolicyRepository;
import site.utnpf.odontolink.domain.repository.InstitutionalSettingsRepository;

/**
 * Bootstrap eager de las filas singleton de configuracion al arrancar la app.
 *
 * <p>Objetivo: garantizar que las tablas singleton ({@code ai_governance_policy},
 * {@code institutional_settings}) tengan su fila id=1 ANTES de que llegue la
 * primera request. Cuando el frontend del admin se monta, dispara varias calls
 * en paralelo ({@code /configuration}, {@code /governance}, {@code /health}) y
 * el viejo patron "lazy bootstrap dentro del use case" sufria un race condition
 * (TOCTOU) que reventaba con {@code Duplicate entry '1' for key PRIMARY}:
 * ambas requests veian {@code findById(1)} vacio, ambas hacian {@code save(...)}
 * con id=1 asignado, una ganaba y la otra fallaba con duplicate key (500).
 *
 * <p>Decision de diseno: corremos al evento {@code ApplicationRunner} (mismo
 * timing que {@link InitialAdminBootstrapper}). En ese momento la app esta lista
 * para recibir trafico pero todavia no llego ninguna request; cualquier
 * insercion aqui evita el race. Si la fila ya existe (deploy posterior, BD
 * pre-cargada por otra instancia, etc.) el bootstrap es no-op.
 *
 * <p>Implementacion con {@link TransactionTemplate} explicito: las anotaciones
 * {@code @Transactional} sobre metodos invocados internamente desde el mismo
 * bean ({@code self-invocation}) NO se aplican porque no pasan por el proxy de
 * Spring. Para no caer en ese pitfall, abrimos la transaccion de forma
 * programatica.
 *
 * <p>Defensa extra: atrapamos {@link DataIntegrityViolationException} dentro de
 * cada bootstrap por si dos instancias del backend levantan a la vez contra la
 * misma BD (futuro escalado horizontal). La excepcion se loguea WARN y la
 * ejecucion continua: el row real ya esta en BD, no hace falta tirar la app.
 *
 * <p>Nota: {@link site.utnpf.odontolink.domain.model.AiAgentConfiguration} NO
 * se bootstrapea porque por diseno no tiene defaults — el admin es responsable
 * de proveer todos los valores en el primer PUT. El controller responde 204
 * cuando no existe la fila (lifecycle virtual UNCONFIGURED).
 */
@Component
public class AiAgentSingletonBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiAgentSingletonBootstrapper.class);

    private final AiGovernancePolicyRepository policyRepository;
    private final InstitutionalSettingsRepository settingsRepository;
    private final TransactionTemplate txTemplate;

    public AiAgentSingletonBootstrapper(AiGovernancePolicyRepository policyRepository,
                                        InstitutionalSettingsRepository settingsRepository,
                                        PlatformTransactionManager transactionManager) {
        this.policyRepository = policyRepository;
        this.settingsRepository = settingsRepository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapGovernancePolicy();
        bootstrapInstitutionalSettings();
    }

    /**
     * Siembra la fila singleton de {@link AiGovernancePolicy} con valores
     * estrictos por defecto si todavia no existe. Idempotente y seguro contra
     * race entre instancias.
     */
    private void bootstrapGovernancePolicy() {
        try {
            txTemplate.executeWithoutResult(status -> {
                if (policyRepository.findSingleton().isPresent()) {
                    return;
                }
                policyRepository.save(AiGovernancePolicy.defaultStrict());
                log.info("AiGovernancePolicy: fila singleton creada con defaults estrictos.");
            });
        } catch (DataIntegrityViolationException ex) {
            // Otra instancia/transaccion gano la carrera. La fila ya esta;
            // continuamos sin error.
            log.warn("AiGovernancePolicy: el bootstrap detecto que la fila ya existia "
                    + "(creada concurrentemente). Continuamos.", ex);
        }
    }

    /**
     * Siembra la fila singleton de {@link InstitutionalSettings} con valores
     * neutros si todavia no existe. Mismo patron idempotente.
     */
    private void bootstrapInstitutionalSettings() {
        try {
            txTemplate.executeWithoutResult(status -> {
                if (settingsRepository.findSingleton().isPresent()) {
                    return;
                }
                settingsRepository.save(InstitutionalSettings.defaults());
                log.info("InstitutionalSettings: fila singleton creada con defaults neutros.");
            });
        } catch (DataIntegrityViolationException ex) {
            log.warn("InstitutionalSettings: el bootstrap detecto que la fila ya existia "
                    + "(creada concurrentemente). Continuamos.", ex);
        }
    }
}
