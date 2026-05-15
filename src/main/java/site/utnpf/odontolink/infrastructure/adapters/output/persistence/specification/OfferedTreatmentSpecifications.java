package site.utnpf.odontolink.infrastructure.adapters.output.persistence.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.OfferedTreatmentStatus;
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
     * Filtro {@code status = ACTIVE}. Garantía base del catálogo público:
     * sólo las ofertas bookables son visibles. PAUSED e INACTIVE quedan ocultas.
     */
    public static Specification<OfferedTreatmentEntity> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), OfferedTreatmentStatus.ACTIVE);
    }

    /**
     * Filtro de vigencia temporal: la fecha actual debe caer dentro de
     * {@code [offerStartDate, offerEndDate]}. Las ofertas con fechas null
     * son tolerantes (no caducan automáticamente) — históricamente la
     * obligatoriedad la enforza el constructor del POJO en alta nueva,
     * y filas legacy podrían carecer de fechas.
     *
     * Es la garantía complementaria a {@link #isActive()} para que el
     * catálogo público no muestre ofertas vencidas por tiempo. La oferta
     * permanece ACTIVE en BD; el servidor la oculta hasta que el practicante
     * extienda la vigencia o la dé de baja.
     */
    public static Specification<OfferedTreatmentEntity> isWithinTemporalWindow(java.time.LocalDate today) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Predicate startOk = cb.or(
                    cb.isNull(root.get("offerStartDate")),
                    cb.lessThanOrEqualTo(root.get("offerStartDate"), today)
            );
            jakarta.persistence.criteria.Predicate endOk = cb.or(
                    cb.isNull(root.get("offerEndDate")),
                    cb.greaterThanOrEqualTo(root.get("offerEndDate"), today)
            );
            return cb.and(startOk, endOk);
        };
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
     *
     * Por qué NO se usa {@code query.distinct(true)} aqui: las tres asociaciones
     * involucradas ({@code treatment}, {@code practitioner}, {@code user}) son
     * {@code @ManyToOne}/{@code @OneToOne}, asi que cada OfferedTreatment aparece
     * exactamente una vez por estos joins. Activar DISTINCT no solo es innecesario
     * sino que rompe el ORDER BY bajo MySQL {@code ONLY_FULL_GROUP_BY}: la columna
     * de orden (p.ej. {@code t.name}) no aparece en el SELECT y MySQL rechaza la
     * query con {@code SQLSTATE HY000 / Error 3065}.
     */
    public static Specification<OfferedTreatmentEntity> keywordIn(String keyword) {
        final String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
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
     * Implementacion: subquery {@code EXISTS} en lugar de {@code INNER JOIN} +
     * {@code DISTINCT}. La asociacion {@code availabilitySlots} es {@code @OneToMany},
     * asi que un join inner producia una fila por cada slot que matcheara y forzaba
     * a usar DISTINCT para deduplicar. Con DISTINCT activo, MySQL bajo
     * {@code ONLY_FULL_GROUP_BY} (modo por defecto desde 5.7) rechaza cualquier
     * {@code ORDER BY} sobre una columna que no este en el SELECT — por ejemplo,
     * ordenar por {@code treatment.name} dispara {@code SQLSTATE HY000 / Error 3065}:
     * "Expression #1 of ORDER BY clause is not in SELECT list ... incompatible with DISTINCT".
     *
     * EXISTS evita ese camino: filtra sin multiplicar filas, no requiere DISTINCT,
     * y deja al planner abortar la subquery en cuanto encuentra el primer match
     * (mejor performance que un join + group-by sobre tablas grandes). Como bonus,
     * elimina el riesgo de duplicar el conteo de pagina cuando se combinan varios
     * filtros que tocan colecciones {@code @OneToMany}.
     */
    public static Specification<OfferedTreatmentEntity> hasAvailabilityOn(java.time.DayOfWeek day) {
        return (root, query, cb) -> {
            // query == null no deberia ocurrir bajo Spring Data JPA, pero la API
            // formal lo permite; en ese caso no se puede construir la subquery,
            // asi que devolvemos un predicado tautologico (no aplicar filtro).
            if (query == null) {
                return cb.conjunction();
            }
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<AvailabilitySlotEntity> slot = subquery.from(AvailabilitySlotEntity.class);
            subquery.select(cb.literal(1));
            subquery.where(
                    cb.equal(slot.get("offeredTreatment"), root),
                    cb.equal(slot.get("dayOfWeek"), day)
            );
            return cb.exists(subquery);
        };
    }

    /**
     * Compone una Specification a partir de los criterios opcionales.
     * Cada filtro se agrega sólo si está presente; el resultado siempre
     * incluye {@link #isActive()} y {@link #isWithinTemporalWindow}
     * como cláusulas base — el catálogo público nunca expone ofertas
     * vencidas por tiempo ni en estados no-bookables.
     */
    public static Specification<OfferedTreatmentEntity> fromCriteria(OfferedTreatmentSearchCriteria criteria) {
        Specification<OfferedTreatmentEntity> spec =
                isActive().and(isWithinTemporalWindow(java.time.LocalDate.now()));
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
