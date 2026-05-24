package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackCriterionCodes;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.FeedbackDirectionalAggregates;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.PractitionerCriterionPerformance;
import site.utnpf.odontolink.domain.model.PractitionerRankingEntry;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AttentionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackCriterionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackCriterionScoreEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaFeedbackRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.FeedbackPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.specification.FeedbackSpecifications;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Feedback (hexagonal output adapter).
 *
 * <p>Concentra:
 * <ul>
 *   <li>CRUD básico vía Spring Data + mapper.</li>
 *   <li>Búsqueda paginada del Panel Docente vía
 *       {@link FeedbackSpecifications}.</li>
 *   <li>Agregados legacy por dirección, ahora calculados sobre el score del
 *       criterio "holístico" de cada dirección
 *       ({@link FeedbackCriterionCodes#GENERAL_SATISFACTION} y
 *       {@link FeedbackCriterionCodes#PATIENT_BEHAVIOR}).</li>
 *   <li>Queries analíticas para los charts del panel docente
 *       (top-N por criterio, ranking combinado).</li>
 * </ul>
 */
@Component
@Transactional(readOnly = true)
public class FeedbackPersistenceAdapter implements FeedbackRepository {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "createdAt",       "createdAt",
            "practitionerId",  "attention.practitioner.id",
            "patientId",       "attention.patient.id",
            "treatmentId",     "attention.treatment.id",
            "id",              "id"
    );

    private final JpaFeedbackRepository jpaFeedbackRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public FeedbackPersistenceAdapter(JpaFeedbackRepository jpaFeedbackRepository) {
        this.jpaFeedbackRepository = jpaFeedbackRepository;
    }

    @Override
    @Transactional
    public Feedback save(Feedback feedback) {
        var entity = FeedbackPersistenceMapper.toEntity(feedback);
        var savedEntity = jpaFeedbackRepository.save(entity);
        return FeedbackPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Feedback> findById(Long id) {
        return jpaFeedbackRepository.findById(id)
                .map(FeedbackPersistenceMapper::toDomain);
    }

    @Override
    public List<Feedback> findByAttention(Attention attention) {
        return findByAttentionId(attention.getId());
    }

    @Override
    public List<Feedback> findByAttentionId(Long attentionId) {
        return jpaFeedbackRepository.findByAttention_Id(attentionId).stream()
                .map(FeedbackPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAttentionAndSubmittedBy(Attention attention, User submittedBy) {
        return jpaFeedbackRepository.existsByAttention_IdAndSubmittedBy_Id(
                attention.getId(),
                submittedBy.getId()
        );
    }

    @Override
    public List<Feedback> findBySubmittedById(Long userId) {
        return jpaFeedbackRepository.findBySubmittedBy_Id(userId).stream()
                .map(FeedbackPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Feedback> findByAttentionIdAndSubmittedById(Long attentionId, Long userId) {
        return jpaFeedbackRepository.findByAttention_IdAndSubmittedBy_Id(attentionId, userId)
                .map(FeedbackPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Feedback> searchDashboard(FeedbackSearchCriteria criteria, PageQuery pageQuery) {
        Specification<FeedbackEntity> spec = FeedbackSpecifications.fromCriteria(criteria);
        Pageable pageable = toSpringPageable(pageQuery);

        Page<FeedbackEntity> page = jpaFeedbackRepository.findAll(spec, pageable);

        List<Feedback> content = page.getContent().stream()
                .map(FeedbackPersistenceMapper::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public FeedbackDirectionalAggregates aggregateByDirection(FeedbackSearchCriteria criteria) {
        Set<Long> allowed = criteria.getAllowedPractitionerIds();
        if (allowed == null || allowed.isEmpty()) {
            return FeedbackDirectionalAggregates.empty();
        }

        FeedbackSearchCriteria p2p = criteria.withDirection(FeedbackDirection.PATIENT_TO_PRACTITIONER);
        FeedbackSearchCriteria pra2pat = criteria.withDirection(FeedbackDirection.PRACTITIONER_TO_PATIENT);

        double[] p2pAgg = avgAndCount(p2p, FeedbackCriterionCodes.GENERAL_SATISFACTION);
        double[] pra2patAgg = avgAndCount(pra2pat, FeedbackCriterionCodes.PATIENT_BEHAVIOR);

        return new FeedbackDirectionalAggregates(
                p2pAgg[0],
                (long) p2pAgg[1],
                pra2patAgg[0],
                (long) pra2patAgg[1]
        );
    }

    /**
     * AVG(score) y COUNT(feedback) sobre el universo derivado del criteria,
     * restringido al criterio "holístico" de la dirección
     * ({@link FeedbackCriterionCodes#GENERAL_SATISFACTION} para P→Pr,
     * {@link FeedbackCriterionCodes#PATIENT_BEHAVIOR} para Pr→Pat). AVG sobre
     * conjunto vacío en JPA retorna {@code null}: lo normalizamos a 0.0.
     */
    private double[] avgAndCount(FeedbackSearchCriteria criteria, String criterionCode) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<FeedbackEntity> root = cq.from(FeedbackEntity.class);

        Join<FeedbackEntity, FeedbackCriterionScoreEntity> scoreJoin =
                root.join("scores", JoinType.INNER);
        Join<FeedbackCriterionScoreEntity, FeedbackCriterionEntity> criterionJoin =
                scoreJoin.join("criterion", JoinType.INNER);

        Specification<FeedbackEntity> spec = FeedbackSpecifications.fromCriteria(criteria);
        Predicate filterPredicate = spec.toPredicate(root, cq, cb);
        Predicate criterionPredicate = cb.equal(criterionJoin.get("code"), criterionCode);
        Predicate finalPredicate = filterPredicate != null
                ? cb.and(filterPredicate, criterionPredicate)
                : criterionPredicate;
        cq.where(finalPredicate);

        cq.multiselect(cb.avg(scoreJoin.get("score")), cb.countDistinct(root));
        Object[] row = entityManager.createQuery(cq).getSingleResult();

        Double avg = (Double) row[0];
        Long count = (Long) row[1];
        return new double[] {
                avg != null ? avg : 0.0,
                count != null ? count : 0L
        };
    }

    @Override
    public List<PractitionerCriterionPerformance> topPractitionersByCriterion(
            String criterionCode,
            Set<Long> allowedPractitionerIds,
            LocalDate startDate,
            LocalDate endDate,
            Long treatmentId,
            int minFeedbackCount,
            int topN) {

        if (allowedPractitionerIds == null || allowedPractitionerIds.isEmpty() || topN <= 0) {
            return Collections.emptyList();
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<FeedbackCriterionScoreEntity> scoreRoot = cq.from(FeedbackCriterionScoreEntity.class);

        Join<FeedbackCriterionScoreEntity, FeedbackCriterionEntity> criterionJoin =
                scoreRoot.join("criterion", JoinType.INNER);
        Join<FeedbackCriterionScoreEntity, FeedbackEntity> feedbackJoin =
                scoreRoot.join("feedback", JoinType.INNER);
        Join<FeedbackEntity, AttentionEntity> attentionJoin =
                feedbackJoin.join("attention", JoinType.INNER);
        Join<AttentionEntity, PractitionerEntity> practitionerJoin =
                attentionJoin.join("practitioner", JoinType.INNER);
        Join<PractitionerEntity, UserEntity> userJoin =
                practitionerJoin.join("user", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(criterionJoin.get("code"), criterionCode));
        predicates.add(practitionerJoin.get("id").in(allowedPractitionerIds));
        addOptionalDateRange(cb, predicates, feedbackJoin.get("createdAt"), startDate, endDate);
        if (treatmentId != null) {
            Join<AttentionEntity, ?> treatmentJoin = attentionJoin.join("treatment", JoinType.INNER);
            predicates.add(cb.equal(treatmentJoin.get("id"), treatmentId));
        }

        cq.multiselect(
                practitionerJoin.get("id"),
                userJoin.get("firstName"),
                userJoin.get("lastName"),
                cb.avg(scoreRoot.get("score")),
                cb.count(scoreRoot)
        );
        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(
                practitionerJoin.get("id"),
                userJoin.get("firstName"),
                userJoin.get("lastName")
        );
        cq.having(cb.greaterThanOrEqualTo(cb.count(scoreRoot), (long) minFeedbackCount));
        cq.orderBy(cb.desc(cb.avg(scoreRoot.get("score"))));

        List<Object[]> rows = entityManager.createQuery(cq)
                .setMaxResults(topN)
                .getResultList();

        String criterionDisplayName = resolveCriterionDisplayName(criterionCode);

        List<PractitionerCriterionPerformance> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] row : rows) {
            Long practitionerId = (Long) row[0];
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            double avg = ((Number) row[3]).doubleValue();
            long count = ((Number) row[4]).longValue();
            String fullName = (firstName == null ? "" : firstName) + " "
                    + (lastName == null ? "" : lastName);
            result.add(new PractitionerCriterionPerformance(
                    practitionerId,
                    fullName.trim(),
                    criterionCode,
                    criterionDisplayName,
                    avg,
                    count,
                    rank++
            ));
        }
        return result;
    }

    @Override
    public List<PractitionerRankingEntry> practitionerOverallRanking(
            Set<Long> allowedPractitionerIds,
            LocalDate startDate,
            LocalDate endDate,
            Long treatmentId,
            int minFeedbackCount,
            int topN) {

        if (allowedPractitionerIds == null || allowedPractitionerIds.isEmpty() || topN <= 0) {
            return Collections.emptyList();
        }

        // Paso 1: traer per-(practitioner, criterio) AVG y COUNT sobre los
        // criterios includeInRanking=true y direction=P→Pr. La agregación
        // "promedio de promedios" se hace en Java porque MySQL no soporta
        // nested aggregates en una sola query sin derived table — y la
        // expresividad de JPA Criteria sobre derived tables es limitada.
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<FeedbackCriterionScoreEntity> scoreRoot = cq.from(FeedbackCriterionScoreEntity.class);

        Join<FeedbackCriterionScoreEntity, FeedbackCriterionEntity> criterionJoin =
                scoreRoot.join("criterion", JoinType.INNER);
        Join<FeedbackCriterionScoreEntity, FeedbackEntity> feedbackJoin =
                scoreRoot.join("feedback", JoinType.INNER);
        Join<FeedbackEntity, AttentionEntity> attentionJoin =
                feedbackJoin.join("attention", JoinType.INNER);
        Join<AttentionEntity, PractitionerEntity> practitionerJoin =
                attentionJoin.join("practitioner", JoinType.INNER);
        Join<PractitionerEntity, UserEntity> userJoin =
                practitionerJoin.join("user", JoinType.INNER);
        Join<FeedbackEntity, UserEntity> submittedByJoin =
                feedbackJoin.join("submittedBy", JoinType.INNER);
        Join<AttentionEntity, PatientEntity> patientJoin =
                attentionJoin.join("patient", JoinType.INNER);
        Join<PatientEntity, UserEntity> patientUserJoin =
                patientJoin.join("user", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(criterionJoin.get("includeInRanking")));
        predicates.add(cb.isTrue(criterionJoin.get("active")));
        predicates.add(cb.equal(criterionJoin.get("applicableDirection"),
                FeedbackDirection.PATIENT_TO_PRACTITIONER));
        // El feedback debe ser efectivamente P→Pr: el submittedBy es el user
        // del paciente. Defensa adicional contra posibles inconsistencias.
        predicates.add(cb.equal(submittedByJoin.get("id"), patientUserJoin.get("id")));
        predicates.add(practitionerJoin.get("id").in(allowedPractitionerIds));
        addOptionalDateRange(cb, predicates, feedbackJoin.get("createdAt"), startDate, endDate);
        if (treatmentId != null) {
            Join<AttentionEntity, ?> treatmentJoin = attentionJoin.join("treatment", JoinType.INNER);
            predicates.add(cb.equal(treatmentJoin.get("id"), treatmentId));
        }

        cq.multiselect(
                practitionerJoin.get("id"),
                userJoin.get("firstName"),
                userJoin.get("lastName"),
                criterionJoin.get("code"),
                criterionJoin.get("displayName"),
                cb.avg(scoreRoot.get("score")),
                cb.countDistinct(feedbackJoin.get("id"))
        );
        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(
                practitionerJoin.get("id"),
                userJoin.get("firstName"),
                userJoin.get("lastName"),
                criterionJoin.get("code"),
                criterionJoin.get("displayName")
        );

        List<Object[]> rows = entityManager.createQuery(cq).getResultList();

        // Agregamos en memoria: por practicante mantenemos el nombre, el
        // mapa de promedios por criterio (LinkedHashMap para preservar
        // displayOrder lógico, aunque acá viene por GROUP BY — irrelevante)
        // y el feedbackCount distinto (máximo entre criterios: si en algún
        // criterio el practicante tuvo más feedbacks distintos, ese es el
        // # de encuestas que recibió).
        Map<Long, PractitionerAccumulator> byPractitioner = new HashMap<>();
        for (Object[] row : rows) {
            Long practitionerId = (Long) row[0];
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String criterionCode = (String) row[3];
            Double avg = row[5] != null ? ((Number) row[5]).doubleValue() : null;
            long distinctFeedbacks = ((Number) row[6]).longValue();

            PractitionerAccumulator acc = byPractitioner.computeIfAbsent(
                    practitionerId,
                    id -> new PractitionerAccumulator(buildFullName(firstName, lastName))
            );
            if (avg != null) {
                acc.perCriterionAverages.put(criterionCode, avg);
            }
            if (distinctFeedbacks > acc.feedbackCount) {
                acc.feedbackCount = distinctFeedbacks;
            }
        }

        // Filtrar por umbral mínimo y ordenar.
        List<PractitionerRankingEntry> ranked = new ArrayList<>();
        byPractitioner.forEach((practitionerId, acc) -> {
            if (acc.feedbackCount < minFeedbackCount || acc.perCriterionAverages.isEmpty()) {
                return;
            }
            double combined = acc.perCriterionAverages.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            ranked.add(new PractitionerRankingEntry(
                    practitionerId,
                    acc.practitionerName,
                    combined,
                    acc.perCriterionAverages,
                    acc.feedbackCount,
                    0 // rank position se asigna abajo
            ));
        });
        ranked.sort((a, b) -> Double.compare(b.getCombinedAverage(), a.getCombinedAverage()));

        List<PractitionerRankingEntry> finalList = new ArrayList<>();
        int rank = 1;
        for (PractitionerRankingEntry entry : ranked) {
            if (rank > topN) {
                break;
            }
            finalList.add(new PractitionerRankingEntry(
                    entry.getPractitionerId(),
                    entry.getPractitionerName(),
                    entry.getCombinedAverage(),
                    entry.getPerCriterionAverages(),
                    entry.getFeedbackCount(),
                    rank++
            ));
        }
        return finalList;
    }

    private void addOptionalDateRange(CriteriaBuilder cb,
                                      List<Predicate> predicates,
                                      jakarta.persistence.criteria.Path<java.time.Instant> createdAt,
                                      LocalDate startDate,
                                      LocalDate endDate) {
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    createdAt,
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    createdAt,
                    endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()));
        }
    }

    private String resolveCriterionDisplayName(String code) {
        // Lookup ligero, sólo se invoca en el top-N path (≤topN ejecuciones).
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<FeedbackCriterionEntity> root = cq.from(FeedbackCriterionEntity.class);
        cq.select(root.get("displayName")).where(cb.equal(root.get("code"), code));
        try {
            return entityManager.createQuery(cq).setMaxResults(1).getSingleResult();
        } catch (jakarta.persistence.NoResultException ex) {
            return code;
        }
    }

    private static String buildFullName(String firstName, String lastName) {
        String f = firstName == null ? "" : firstName;
        String l = lastName == null ? "" : lastName;
        return (f + " " + l).trim();
    }

    private Pageable toSpringPageable(PageQuery pageQuery) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (pageQuery.hasSort()) {
            String alias = pageQuery.getSortBy();
            String entityPath = ALLOWED_SORT_FIELDS.get(alias);
            if (entityPath != null) {
                Sort.Direction direction = pageQuery.getSortDirection() == PageQuery.SortDirection.DESC
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                sort = Sort.by(direction, entityPath);
            }
        }
        return PageRequest.of(pageQuery.getPage(), pageQuery.getSize(), sort);
    }

    public static Set<String> allowedSortAliases() {
        return new LinkedHashSet<>(ALLOWED_SORT_FIELDS.keySet());
    }

    /**
     * Acumulador interno mutable para colapsar las filas Object[] del query
     * de ranking en una sola entrada por practicante.
     */
    private static final class PractitionerAccumulator {
        final String practitionerName;
        final Map<String, Double> perCriterionAverages = new LinkedHashMap<>();
        long feedbackCount;

        PractitionerAccumulator(String practitionerName) {
            this.practitionerName = practitionerName;
        }
    }
}
