package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackDirection;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para el catálogo de {@link FeedbackCriterion} (master data
 * de los criterios sobre los que se puntúa un feedback). Sigue el patrón
 * hexagonal del proyecto.
 */
public interface FeedbackCriterionRepository {

    /**
     * Devuelve los criterios ACTIVOS aplicables a una dirección, ordenados
     * por {@code displayOrder} ascendente. Es la consulta que el frontend
     * usa para renderizar el formulario de la encuesta.
     */
    List<FeedbackCriterion> findActiveByDirection(FeedbackDirection direction);

    /**
     * Devuelve los criterios ACTIVOS con {@code includeInRanking=true}
     * aplicables a una dirección, ordenados por {@code displayOrder}.
     * Usado por el ranking combinado del panel docente.
     */
    List<FeedbackCriterion> findActiveRankingByDirection(FeedbackDirection direction);

    /**
     * Devuelve el catálogo completo (activos e inactivos) ordenado por
     * {@code displayOrder}. Reservado para usos administrativos futuros.
     */
    List<FeedbackCriterion> findAll();

    Optional<FeedbackCriterion> findByCode(String code);

    boolean existsByCode(String code);

    FeedbackCriterion save(FeedbackCriterion criterion);
}
