package site.utnpf.odontolink.infrastructure.adapters.output.persistence.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AvailabilitySlotEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.OfferedTreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;

/**
 * Construcción de predicados dinámicos (Criteria API + Specifications)
 * para el motor de búsqueda del catálogo público (RF09).
 *
 * Decisión arquitectónica: Specifications, no @Query JPQL.
 * - Permite componer filtros opcionales sin combinatoria explosiva de
 *   métodos del repositorio (con N filtros el enfoque @Query exige 2^N
 *   variantes o JPQL con if-vacios sobre parámetros nulos, difícil de
 *   mantener y susceptible a errores de string).
 * - Mantiene la lógica de query type-safe y refactor-friendly: si un
 *   atributo cambia de nombre, el compilador avisa.
 *
 * Importante: la Specification {@code isActive} se aplica SIEMPRE en el
 * adaptador. Las Specifications de este archivo NO la incluyen para que
 * cada combinador sea ortogonal y reusable también desde flujos
 * administrativos (donde un admin podría querer ver inactivas).
 */
public final class OfferedTreatmentSpecifications {

    private OfferedTreatmentSpecifications() {
        // Utility class
    }

    /**
     * Filtro {@code active = true}. Garantía base del catálogo público.
     */
    public static Specification<OfferedTreatmentEntity> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Filtro por palabra clave: busca coincidencias parciales y case-insensitive
     * en el nombre/descripción del Treatment y en firstName/lastName del User
     * detrás del Practitioner.
     *
     * Por qué LOWER() + LIKE: MySQL con collation *_ci ya es insensible a mayúsculas,
     * pero forzar LOWER en ambos lados garantiza el comportamiento si el schema se
     * porta a un motor con collation case-sensitive (Postgres por defecto). El
     * costo es bajo porque las columnas involucradas son cortas.
     *
     * Por qué LEFT JOIN sobre user: aunque {@code practitioner.user} es NOT NULL
     * por restricción de FK, el LEFT JOIN evita que un dato corrupto haga
     * desaparecer ofertas del resultado durante una búsqueda; preferimos
     * tolerancia en el catálogo público.
     */
    public static Specification<OfferedTreatmentEntity> keywordIn(String keyword) {
        final String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
            // distinct() evita duplicados cuando el join a slots se combine
            // con éste en una misma búsqueda multi-filtro.
            if (query != null) {
                query.distinct(true);
            }

            Join<OfferedTreatmentEntity, TreatmentEntity> treatmentJoin =
                    root.join("treatment", JoinType.LEFT);
            Join<OfferedTreatmentEntity, PractitionerEntity> practitionerJoin =
                    root.join("practitioner", JoinType.LEFT);
            Join<PractitionerEntity, UserEntity> userJoin =
                    practitionerJoin.join("user", JoinType.LEFT);

            Predicate treatmentNameMatch =
                    cb.like(cb.lower(treatmentJoin.get("name")), pattern);
            Predicate treatmentDescriptionMatch =
                    cb.like(cb.lower(treatmentJoin.get("description")), pattern);
            Predicate practitionerFirstNameMatch =
                    cb.like(cb.lower(userJoin.get("firstName")), pattern);
            Predicate practitionerLastNameMatch =
                    cb.like(cb.lower(userJoin.get("lastName")), pattern);

            return cb.or(
                    treatmentNameMatch,
                    treatmentDescriptionMatch,
                    practitionerFirstNameMatch,
                    practitionerLastNameMatch
            );
        };
    }

    /**
     * Filtro por especialidad exacta (área odontológica del Treatment).
     * Se aplica con igualdad case-insensitive para tolerar variaciones de
     * casing en el frontend ("Ortodoncia" vs "ORTODONCIA").
     */
    public static Specification<OfferedTreatmentEntity> hasSpecialty(String specialty) {
        final String normalized = specialty.toLowerCase();
        return (root, query, cb) -> {
            Join<OfferedTreatmentEntity, TreatmentEntity> treatmentJoin =
                    root.join("treatment", JoinType.LEFT);
            return cb.equal(cb.lower(treatmentJoin.get("area")), normalized);
        };
    }

    /**
     * Filtro por disponibilidad: la oferta debe publicar al menos un
     * AvailabilitySlot en el día solicitado.
     *
     * El distinct() es indispensable: una oferta con tres slots en MONDAY
     * aparecería tres veces sin él.
     */
    public static Specification<OfferedTreatmentEntity> hasAvailabilityOn(java.time.DayOfWeek day) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<OfferedTreatmentEntity, AvailabilitySlotEntity> slotJoin =
                    root.join("availabilitySlots", JoinType.INNER);
            return cb.equal(slotJoin.get("dayOfWeek"), day);
        };
    }

    /**
     * Compone una Specification a partir de los criterios opcionales.
     * Cada filtro se agrega sólo si está presente; el resultado siempre
     * incluye {@link #isActive()} como cláusula base.
     */
    public static Specification<OfferedTreatmentEntity> fromCriteria(OfferedTreatmentSearchCriteria criteria) {
        Specification<OfferedTreatmentEntity> spec = isActive();
        if (criteria == null) {
            return spec;
        }
        if (criteria.hasKeyword()) {
            spec = spec.and(keywordIn(criteria.getKeyword()));
        }
        if (criteria.hasSpecialty()) {
            spec = spec.and(hasSpecialty(criteria.getSpecialty()));
        }
        if (criteria.hasAvailabilityDay()) {
            spec = spec.and(hasAvailabilityOn(criteria.getAvailabilityDay()));
        }
        return spec;
    }
}
