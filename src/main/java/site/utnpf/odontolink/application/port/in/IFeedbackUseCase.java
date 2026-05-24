package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.CriterionScoreInput;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada de los casos de uso MICRO-CONTEXTO de Feedback (RF21,
 * RF22, RF23, RF24). El catálogo de criterios usa
 * {@link IFeedbackCriterionCatalogUseCase}; el panel macro vive en
 * {@link ISupervisorFeedbackDashboardUseCase}.
 */
public interface IFeedbackUseCase {

    /**
     * Crea un feedback multi-criterio sobre una atención finalizada.
     *
     * <p>Validaciones en cascada:
     * <ul>
     *   <li>Atención debe estar COMPLETED.</li>
     *   <li>{@code submittingUser} debe ser el paciente o el practicante de
     *       la atención.</li>
     *   <li>El usuario no debe tener feedback previo (RF23).</li>
     *   <li>Los {@code scores} deben cubrir exactamente el set de criterios
     *       activos para la dirección — validado por
     *       {@link site.utnpf.odontolink.domain.service.FeedbackCriterionPolicyService}.</li>
     *   <li>Cada score individual debe estar en el rango 1–5.</li>
     * </ul>
     */
    Feedback createFeedback(Long attentionId,
                            List<CriterionScoreInput> scores,
                            String comment,
                            User submittingUser);

    List<Feedback> getFeedbackForAttention(Long attentionId, User requestingUser);

    Feedback getFeedbackById(Long feedbackId);
}
