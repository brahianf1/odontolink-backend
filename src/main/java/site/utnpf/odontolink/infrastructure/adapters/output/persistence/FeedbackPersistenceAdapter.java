package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaFeedbackRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.FeedbackPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.specification.FeedbackSpecifications;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Feedback (Hexagonal Architecture).
 * Implementa la interfaz del dominio FeedbackRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Incluye el motor analítico del Panel Docente de Feedback (RF25):
 *  - {@link #searchDashboard} ejecuta la búsqueda paginada usando
 *    {@link FeedbackSpecifications}.
 *  - {@link #averageRating} calcula el promedio en BD sobre el mismo
 *    universo de filtros para evitar traer todas las filas a memoria.
 *
 * Politica transaccional uniforme con el resto de adapters; ver
 * {@link UserPersistenceAdapter} para el racional.
 *
 * @author OdontoLink Team
 */
@Component
@Transactional(readOnly = true)
public class FeedbackPersistenceAdapter implements FeedbackRepository {

    /**
     * Lista blanca de propiedades por las que se permite ordenar el panel
     * docente. Mapea el alias expuesto al cliente (clave) a la ruta JPA real
     * (valor). Se usa como defensa contra inyección de propiedades en el
     * {@code sortBy}: cualquier valor fuera de este allowlist se ignora
     * silenciosamente para no facilitar enumeración de campos.
     */
    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "createdAt",       "createdAt",
            "rating",          "rating",
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
    public double averageRating(FeedbackSearchCriteria criteria) {
        // Atajo de defensa: si el cerco docente-alumno está vacío, sabemos
        // que el universo resultante también lo está. Evitamos un round-trip
        // al motor para preguntarle algo que ya conocemos en memoria.
        Set<Long> allowed = criteria.getAllowedPractitionerIds();
        if (allowed == null || allowed.isEmpty()) {
            return 0.0;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Double> cq = cb.createQuery(Double.class);
        Root<FeedbackEntity> root = cq.from(FeedbackEntity.class);

        // Reutilizamos la composición de filtros de Specifications para
        // garantizar que el promedio se calcule sobre EXACTAMENTE el mismo
        // universo que la búsqueda paginada (consistencia agregado ↔ slice).
        Specification<FeedbackEntity> spec = FeedbackSpecifications.fromCriteria(criteria);
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }

        // AVG en BD: el motor es mucho más eficiente que traer las filas
        // y promediarlas en Java, sobre todo cuando hay miles de feedbacks.
        cq.select(cb.avg(root.get("rating")));

        Double result = entityManager.createQuery(cq).getSingleResult();
        // AVG sobre conjunto vacío retorna null en JPA; lo normalizamos a 0
        // para que el contrato del puerto sea estable (ver Javadoc del port).
        return result != null ? result : 0.0;
    }

    /**
     * Traduce un {@link PageQuery} del Dominio a {@link Pageable} de Spring,
     * aplicando un allowlist sobre {@code sortBy} para evitar exponer
     * propiedades arbitrarias (defense-in-depth contra inyección de campo).
     *
     * Si el cliente no especifica orden, ordenamos por {@code createdAt DESC}
     * por defecto: la lectura natural del panel docente es "lo más reciente
     * primero", coherente con un dashboard de monitoreo.
     */
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

    /**
     * Conjunto de aliases válidos para {@code sortBy}. Se expone para que el
     * adaptador REST documente correctamente las opciones disponibles.
     */
    public static Set<String> allowedSortAliases() {
        return ALLOWED_SORT_FIELDS.keySet();
    }
}
