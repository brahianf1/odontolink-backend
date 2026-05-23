package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackDirection;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de dominio que valida el set de scores entrante contra el
 * catálogo activo de {@link FeedbackCriterion} para una {@link FeedbackDirection}.
 *
 * <p>Concentra la regla "una encuesta debe cubrir exactamente los criterios
 * activos de su dirección, sin duplicados ni intrusos". Vive en el dominio
 * porque expresa contrato de negocio, independiente de persistencia o REST.
 */
public class FeedbackCriterionPolicyService {

    /**
     * Valida un set de pares {@code (criterionCode, score)} contra el catálogo
     * activo de la dirección. Lanza {@link InvalidBusinessRuleException} con
     * mensaje explicativo ante cualquier desvío.
     *
     * @param scoresByCode      scores entrantes; el caller ya garantizó que
     *                          cada score individual está dentro del rango
     *                          numérico válido (1–5) vía construcción.
     * @param direction         dirección del feedback (P→Pr o Pr→Pat).
     * @param activeCriteria    criterios activos para esa dirección, tal cual
     *                          los devuelve el repositorio.
     */
    public void validateScores(Map<String, Integer> scoresByCode,
                               FeedbackDirection direction,
                               List<FeedbackCriterion> activeCriteria) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(activeCriteria, "activeCriteria");
        if (scoresByCode == null || scoresByCode.isEmpty()) {
            throw new InvalidBusinessRuleException(
                    "La encuesta requiere al menos un score.");
        }

        Set<String> expected = activeCriteria.stream()
                .filter(c -> c.getApplicableDirection() == direction)
                .filter(FeedbackCriterion::isActive)
                .map(FeedbackCriterion::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (expected.isEmpty()) {
            throw new InvalidBusinessRuleException(
                    "No hay criterios activos para la dirección " + direction
                            + ". Contacte al administrador.");
        }

        Set<String> received = new HashSet<>(scoresByCode.keySet());

        Set<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(received);
        if (!missing.isEmpty()) {
            throw new InvalidBusinessRuleException(
                    "Faltan scores para los criterios: " + missing);
        }

        Set<String> unknown = new LinkedHashSet<>(received);
        unknown.removeAll(expected);
        if (!unknown.isEmpty()) {
            throw new InvalidBusinessRuleException(
                    "Criterios no aplicables a la dirección " + direction
                            + " o inactivos: " + unknown);
        }
    }
}
