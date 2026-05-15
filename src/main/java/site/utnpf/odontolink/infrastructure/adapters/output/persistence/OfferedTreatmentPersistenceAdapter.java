package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.OfferedTreatmentStatus;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.OfferedTreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaOfferedTreatmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.OfferedTreatmentPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PractitionerPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.TreatmentPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.specification.OfferedTreatmentSpecifications;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para OfferedTreatment (Hexagonal Architecture).
 * Implementa la interfaz del dominio OfferedTreatmentRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * El mapeo entidad-a-dominio recorre asociaciones LAZY pesadas (practitioner,
 * treatment, availabilitySlots), por lo que el adapter declara
 * {@code @Transactional(readOnly = true)} a nivel de clase para asegurar que
 * la sesion siga abierta durante la conversion. Sin esto, deshabilitar OSIV
 * dispara LazyInitializationException en cualquier endpoint que pase por aqui.
 * Las escrituras se reanotan con {@code @Transactional} para no bloquear el flush.
 */
@Component
@Transactional(readOnly = true)
public class OfferedTreatmentPersistenceAdapter implements OfferedTreatmentRepository {

    /**
     * Lista blanca de propiedades por las que se permite ordenar el catálogo
     * público. Mapea el alias expuesto al cliente (clave) a la ruta JPA real
     * (valor). Se usa como defensa contra inyección de propiedades en el
     * {@code sortBy}: cualquier valor fuera de este allowlist se ignora.
     */
    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "treatmentName", "treatment.name",
            "specialty",     "treatment.area",
            "duration",      "durationInMinutes",
            "offerStartDate","offerStartDate",
            "offerEndDate",  "offerEndDate",
            "id",            "id"
    );

    private final JpaOfferedTreatmentRepository jpaOfferedTreatmentRepository;

    public OfferedTreatmentPersistenceAdapter(JpaOfferedTreatmentRepository jpaOfferedTreatmentRepository) {
        this.jpaOfferedTreatmentRepository = jpaOfferedTreatmentRepository;
    }

    @Override
    @Transactional
    public OfferedTreatment save(OfferedTreatment offeredTreatment) {
        OfferedTreatmentEntity entity = OfferedTreatmentPersistenceMapper.toEntity(offeredTreatment);
        OfferedTreatmentEntity savedEntity = jpaOfferedTreatmentRepository.save(entity);
        return OfferedTreatmentPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<OfferedTreatment> findById(Long id) {
        return jpaOfferedTreatmentRepository.findById(id)
                .map(OfferedTreatmentPersistenceMapper::toDomain);
    }

    @Override
    public List<OfferedTreatment> findByPractitioner(Practitioner practitioner) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        return jpaOfferedTreatmentRepository.findByPractitioner(practitionerEntity).stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferedTreatment> findByPractitionerId(Long practitionerId) {
        return jpaOfferedTreatmentRepository.findByPractitionerId(practitionerId).stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPractitionerAndTreatment(Practitioner practitioner, Treatment treatment) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        TreatmentEntity treatmentEntity = TreatmentPersistenceMapper.toEntity(treatment);
        // Sólo cuentan los estados que ocupan el "slot" del par practitioner+treatment
        // (ACTIVE y PAUSED). INACTIVE es histórico y no compite por la unicidad.
        return jpaOfferedTreatmentRepository
                .existsByPractitionerAndTreatmentAndStatusIn(
                        practitionerEntity,
                        treatmentEntity,
                        EnumSet.of(OfferedTreatmentStatus.ACTIVE, OfferedTreatmentStatus.PAUSED)
                );
    }

    @Override
    public Optional<OfferedTreatment> findByPractitionerAndTreatment(Practitioner practitioner, Treatment treatment) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        TreatmentEntity treatmentEntity = TreatmentPersistenceMapper.toEntity(treatment);
        return jpaOfferedTreatmentRepository.findByPractitionerAndTreatment(practitionerEntity, treatmentEntity)
                .map(OfferedTreatmentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaOfferedTreatmentRepository.deleteById(id);
    }

    @Override
    public boolean hasFutureScheduledAppointments(Long offeredTreatmentId) {
        return jpaOfferedTreatmentRepository
                .hasFutureScheduledAppointments(offeredTreatmentId, LocalDateTime.now());
    }

    @Override
    public boolean hasInProgressAttentions(Long offeredTreatmentId) {
        return jpaOfferedTreatmentRepository.hasInProgressAttentions(offeredTreatmentId);
    }

    @Override
    public boolean hasHistoricalAttentions(Long offeredTreatmentId) {
        return jpaOfferedTreatmentRepository.hasAnyAttentions(offeredTreatmentId);
    }

    @Override
    public List<OfferedTreatment> findAll() {
        // Catálogo público sin filtros: sólo expone ofertas bookables (ACTIVE).
        // PAUSED e INACTIVE quedan ocultas.
        return jpaOfferedTreatmentRepository.findByStatus(OfferedTreatmentStatus.ACTIVE).stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferedTreatment> findByTreatmentId(Long treatmentId) {
        return jpaOfferedTreatmentRepository
                .findByTreatmentIdAndStatus(treatmentId, OfferedTreatmentStatus.ACTIVE).stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<OfferedTreatment> search(OfferedTreatmentSearchCriteria criteria, PageQuery pageQuery) {
        // Spec base: active=true + filtros opcionales del criterio.
        Specification<OfferedTreatmentEntity> spec =
                OfferedTreatmentSpecifications.fromCriteria(criteria);

        Pageable pageable = toSpringPageable(pageQuery);

        Page<OfferedTreatmentEntity> page = jpaOfferedTreatmentRepository.findAll(spec, pageable);

        List<OfferedTreatment> content = page.getContent().stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Traduce un {@link PageQuery} del Dominio a {@link Pageable} de Spring,
     * aplicando un allowlist sobre {@code sortBy} para evitar exponer
     * propiedades arbitrarias (defense-in-depth contra inyección de campo).
     */
    private Pageable toSpringPageable(PageQuery pageQuery) {
        Sort sort = Sort.unsorted();
        if (pageQuery.hasSort()) {
            String alias = pageQuery.getSortBy();
            String entityPath = ALLOWED_SORT_FIELDS.get(alias);
            // Aliases no autorizados se ignoran silenciosamente para no
            // facilitar enumeración de campos válidos al atacante.
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
