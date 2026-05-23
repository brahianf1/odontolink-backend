package site.utnpf.odontolink.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackCriterionCodes;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.repository.FeedbackCriterionRepository;

import java.util.List;

/**
 * Seed eager del catálogo de {@link FeedbackCriterion} al arrancar la app.
 *
 * <p>Mismo patrón que {@link AiAgentSingletonBootstrapper}: corre en
 * {@code ApplicationRunner}, abre transacción programática, atrapa
 * {@link DataIntegrityViolationException} a WARN para soportar arranque
 * concurrente de múltiples instancias contra la misma BD.
 *
 * <p>Decisión idempotente por código: si el {@code code} ya existe, este
 * bootstrapper NO toca la fila. Eso preserva ediciones administrativas
 * futuras (cambio de displayName, includeInRanking, etc.) sin sobrescribir
 * tras cada deploy.
 */
@Component
public class FeedbackCriterionBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FeedbackCriterionBootstrapper.class);

    private final FeedbackCriterionRepository repository;
    private final TransactionTemplate txTemplate;

    public FeedbackCriterionBootstrapper(FeedbackCriterionRepository repository,
                                         PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        List<FeedbackCriterion> defaults = List.of(
                new FeedbackCriterion(
                        FeedbackCriterionCodes.PUNCTUALITY,
                        "Puntualidad",
                        "Cumplimiento de horarios pactados.",
                        FeedbackDirection.PATIENT_TO_PRACTITIONER,
                        true, 1, true),
                new FeedbackCriterion(
                        FeedbackCriterionCodes.CARE_QUALITY,
                        "Calidad de atención",
                        "Cuidado profesional durante el procedimiento.",
                        FeedbackDirection.PATIENT_TO_PRACTITIONER,
                        true, 2, true),
                new FeedbackCriterion(
                        FeedbackCriterionCodes.COMMUNICATION_CLARITY,
                        "Claridad en la comunicación",
                        "Explicación de diagnóstico, plan de tratamiento e indicaciones.",
                        FeedbackDirection.PATIENT_TO_PRACTITIONER,
                        true, 3, true),
                new FeedbackCriterion(
                        FeedbackCriterionCodes.GENERAL_SATISFACTION,
                        "Satisfacción general",
                        "Valoración holística de la experiencia. No participa del ranking.",
                        FeedbackDirection.PATIENT_TO_PRACTITIONER,
                        false, 4, true),
                new FeedbackCriterion(
                        FeedbackCriterionCodes.PATIENT_BEHAVIOR,
                        "Comportamiento general del paciente",
                        "Observación del practicante sobre la conducta del paciente durante la atención.",
                        FeedbackDirection.PRACTITIONER_TO_PATIENT,
                        false, 1, true)
        );
        for (FeedbackCriterion criterion : defaults) {
            seed(criterion);
        }
    }

    private void seed(FeedbackCriterion criterion) {
        try {
            txTemplate.executeWithoutResult(status -> {
                if (repository.existsByCode(criterion.getCode())) {
                    return;
                }
                repository.save(criterion);
                log.info("FeedbackCriterion: sembrado {} ({}).",
                        criterion.getCode(), criterion.getDisplayName());
            });
        } catch (DataIntegrityViolationException ex) {
            // Otra instancia ganó la carrera; la fila ya está.
            log.warn("FeedbackCriterion {}: insertado concurrentemente por otra instancia. Continuamos.",
                    criterion.getCode(), ex);
        }
    }
}
