package site.utnpf.odontolink.infrastructure.adapters.output.persistence.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AttentionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Construcción de predicados dinámicos (Criteria API + Specifications)
 * para el Panel Docente de Supervisión de Feedback (RF25).
 *
 * Decisión arquitectónica: replicamos el patrón ya validado en RF09
 * ({@link OfferedTreatmentSpecifications}) por las mismas razones:
 *  - Cada filtro del docente es opcional; con N filtros, una solución
 *    basada en {@code @Query} requeriría 2^N variantes o JPQL con
 *    pseudo-condicionales sobre parámetros nulos, difícil de mantener.
 *  - El compilador valida los nombres de propiedades de la entidad: si un
 *    atributo cambia, el build rompe ANTES de ejecutarse en producción.
 *
 * Cerco de seguridad: {@link #practitionerScope} se aplica SIEMPRE desde
 * {@link #fromCriteria}. Si la lista permitida está vacía, el resultado se
 * fuerza a vacío (predicado contradictorio) — un supervisor sin alumnos
 * vinculados NUNCA puede ver feedback.
 */
public final class FeedbackSpecifications {

    private FeedbackSpecifications() {
        // Utility class
    }

    /**
     * Cerco docente-alumno: restringe el universo al conjunto de practicantes
     * que el supervisor autenticado tiene vinculados. Es el predicado que
     * garantiza el aislamiento entre supervisores.
     *
     * Si el conjunto está vacío se devuelve un predicado contradictorio
     * ({@code 1 = 0}) para que la query no retorne nada. Una alternativa
     * sería lanzar excepción aguas arriba, pero un resultado vacío es más
     * idempotente para el frontend (caso "docente sin practicantes a
     * cargo" = panel vacío, no error).
     */
    public static Specification<FeedbackEntity> practitionerScope(java.util.Set<Long> allowedPractitionerIds) {
        return (root, query, cb) -> {
            if (allowedPractitionerIds == null || allowedPractitionerIds.isEmpty()) {
                // Predicado siempre falso: vacía el resultado sin romper la query.
                return cb.disjunction();
            }
            Join<FeedbackEntity, AttentionEntity> attentionJoin =
                    root.join("attention", JoinType.INNER);
            Join<AttentionEntity, PractitionerEntity> practitionerJoin =
                    attentionJoin.join("practitioner", JoinType.INNER);
            return practitionerJoin.get("id").in(allowedPractitionerIds);
        };
    }

    /**
     * Filtro por practicante exacto. Se combina con AND sobre el cerco; el
     * caso de "practicante fuera del cerco" se rechaza en la capa de
     * aplicación con UnauthorizedOperation, no aquí.
     */
    public static Specification<FeedbackEntity> hasPractitioner(Long practitionerId) {
        return (root, query, cb) -> {
            Join<FeedbackEntity, AttentionEntity> attentionJoin =
                    root.join("attention", JoinType.INNER);
            Join<AttentionEntity, PractitionerEntity> practitionerJoin =
                    attentionJoin.join("practitioner", JoinType.INNER);
            return cb.equal(practitionerJoin.get("id"), practitionerId);
        };
    }

    /**
     * Filtro por paciente exacto. Permite al docente cruzar feedback de
     * varios practicantes sobre el mismo paciente (útil para detectar
     * patrones de quejas reiteradas).
     */
    public static Specification<FeedbackEntity> hasPatient(Long patientId) {
        return (root, query, cb) -> {
            Join<FeedbackEntity, AttentionEntity> attentionJoin =
                    root.join("attention", JoinType.INNER);
            Join<AttentionEntity, PatientEntity> patientJoin =
                    attentionJoin.join("patient", JoinType.INNER);
            return cb.equal(patientJoin.get("id"), patientId);
        };
    }

    /**
     * Filtro por tratamiento exacto. Sirve para comparar performance de los
     * alumnos en un tipo de procedimiento puntual.
     */
    public static Specification<FeedbackEntity> hasTreatment(Long treatmentId) {
        return (root, query, cb) -> {
            Join<FeedbackEntity, AttentionEntity> attentionJoin =
                    root.join("attention", JoinType.INNER);
            Join<AttentionEntity, TreatmentEntity> treatmentJoin =
                    attentionJoin.join("treatment", JoinType.INNER);
            return cb.equal(treatmentJoin.get("id"), treatmentId);
        };
    }

    /**
     * Filtro inclusivo {@code createdAt >= startDate 00:00:00 (zona del sistema)}.
     *
     * El frontend trabaja con {@link LocalDate} ("dame los feedback del
     * 01/03/2026 en adelante"); el campo en BD es {@link java.time.Instant}.
     * Convertimos la fecha al instante de inicio del día usando la zona
     * por defecto del servidor para coincidir con el calendario del usuario
     * del panel (un docente argentino piensa en hora local).
     */
    public static Specification<FeedbackEntity> createdFrom(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get("createdAt"),
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );
    }

    /**
     * Filtro inclusivo {@code createdAt <= endDate 23:59:59.999... (zona del sistema)}.
     *
     * Tomamos el final del día indicado para que la ventana sea inclusiva en
     * ambos extremos: "1 al 7 de marzo" incluye todo lo registrado el 7.
     */
    public static Specification<FeedbackEntity> createdUntil(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get("createdAt"),
                endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
        );
    }

    /**
     * Filtro por dirección del feedback bidireccional. Compara el
     * {@code submittedBy} del feedback contra el user del paciente o del
     * practicante de la atención para clasificarlo:
     * <ul>
     *   <li>{@link FeedbackDirection#PATIENT_TO_PRACTITIONER}:
     *       {@code feedback.submittedBy = attention.patient.user}.</li>
     *   <li>{@link FeedbackDirection#PRACTITIONER_TO_PATIENT}:
     *       {@code feedback.submittedBy = attention.practitioner.user}.</li>
     * </ul>
     *
     * <p>El join al user del paciente/practicante se realiza en INNER porque
     * todo feedback válido por el modelo de dominio tiene ambos extremos
     * presentes; cualquier null sería inconsistencia y debe excluirse del
     * universo del panel.
     */
    public static Specification<FeedbackEntity> hasDirection(FeedbackDirection direction) {
        return (root, query, cb) -> {
            Join<FeedbackEntity, AttentionEntity> attentionJoin =
                    root.join("attention", JoinType.INNER);
            Join<FeedbackEntity, UserEntity> submittedByJoin =
                    root.join("submittedBy", JoinType.INNER);
            if (direction == FeedbackDirection.PATIENT_TO_PRACTITIONER) {
                Join<AttentionEntity, PatientEntity> patientJoin =
                        attentionJoin.join("patient", JoinType.INNER);
                Join<PatientEntity, UserEntity> patientUserJoin =
                        patientJoin.join("user", JoinType.INNER);
                return cb.equal(submittedByJoin.get("id"), patientUserJoin.get("id"));
            }
            Join<AttentionEntity, PractitionerEntity> practitionerJoin =
                    attentionJoin.join("practitioner", JoinType.INNER);
            Join<PractitionerEntity, UserEntity> practitionerUserJoin =
                    practitionerJoin.join("user", JoinType.INNER);
            return cb.equal(submittedByJoin.get("id"), practitionerUserJoin.get("id"));
        };
    }

    /**
     * Compone la Specification final. Siempre arranca con el cerco docente-
     * alumno; los filtros del docente se agregan sólo si están presentes.
     *
     * <p>El filtro {@code direction} se incluye sólo cuando se aplica a la
     * búsqueda paginada: el método de agregados ({@code aggregateByDirection})
     * pasa por acá pidiendo cada dirección por separado vía
     * {@link FeedbackSearchCriteria#withDirection}, así reutilizamos las
     * mismas Specifications en ambos caminos sin duplicar lógica.
     */
    public static Specification<FeedbackEntity> fromCriteria(FeedbackSearchCriteria criteria) {
        Specification<FeedbackEntity> spec = practitionerScope(criteria.getAllowedPractitionerIds());

        if (criteria.hasPractitionerId()) {
            spec = spec.and(hasPractitioner(criteria.getPractitionerId()));
        }
        if (criteria.hasPatientId()) {
            spec = spec.and(hasPatient(criteria.getPatientId()));
        }
        if (criteria.hasTreatmentId()) {
            spec = spec.and(hasTreatment(criteria.getTreatmentId()));
        }
        if (criteria.hasStartDate()) {
            spec = spec.and(createdFrom(criteria.getStartDate()));
        }
        if (criteria.hasEndDate()) {
            spec = spec.and(createdUntil(criteria.getEndDate()));
        }
        if (criteria.hasDirection()) {
            spec = spec.and(hasDirection(criteria.getDirection()));
        }
        return spec;
    }
}
