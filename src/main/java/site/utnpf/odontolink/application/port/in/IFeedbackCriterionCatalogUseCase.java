package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.domain.model.FeedbackDirection;

import java.util.List;

/**
 * Caso de uso: exponer el catálogo de criterios activos para que el frontend
 * renderice dinámicamente el formulario de la encuesta.
 *
 * <p>El contrato con el FE es <em>code-based</em>: el front no hardcodea
 * códigos. Cuando un criterio nuevo se inserta vía bootstrapper o gestión
 * admin futura, el catálogo lo expone sin redeploy.
 */
public interface IFeedbackCriterionCatalogUseCase {

    /**
     * Criterios activos aplicables a la dirección, ordenados por
     * {@code displayOrder} ascendente.
     */
    List<FeedbackCriterion> listActiveForDirection(FeedbackDirection direction);
}
