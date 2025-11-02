package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada (Input Port) para casos de uso de Feedback.
 * Define las operaciones de negocio disponibles para el sistema de feedback bidireccional.
 *
 * Casos de uso implementados:
 * - CU-009: Calificar Paciente (RF21)
 * - CU-016: Calificar Practicante (RF22)
 * - CU-010: Visualizar Feedback (RF24, RF25, RF40)
 *
 * Siguiendo Arquitectura Hexagonal, este puerto es implementado por el servicio
 * de aplicación (FeedbackService) y utilizado por los adaptadores de entrada (controladores).
 *
 * @author OdontoLink Team
 */
public interface IFeedbackUseCase {

    /**
     * Crea un nuevo feedback sobre una atención finalizada.
     * Implementa CU-009, CU-016 (RF21, RF22, RF23).
     *
     * Validaciones aplicadas:
     * - La atención debe estar en estado COMPLETED
     * - El usuario debe ser el paciente o practicante de la atención
     * - No debe existir un feedback previo del mismo usuario para esa atención (RF23)
     *
     * @param attentionId ID de la atención sobre la que se envía el feedback
     * @param rating Calificación (1-5 estrellas)
     * @param comment Comentario opcional
     * @param submittingUser Usuario que envía el feedback
     * @return El feedback creado
     */
    Feedback createFeedback(Long attentionId, int rating, String comment, User submittingUser);

    /**
     * Obtiene todos los feedbacks asociados a una atención específica.
     * Implementa CU-010 (RF24) - Para pacientes y practicantes.
     *
     * Regla de privacidad:
     * - Solo el paciente o practicante de la atención puede consultar su feedback
     * - Los supervisores pueden consultar el feedback de sus practicantes
     *
     * @param attentionId ID de la atención
     * @param requestingUser Usuario que solicita ver el feedback
     * @return Lista de feedbacks de la atención
     */
    List<Feedback> getFeedbackForAttention(Long attentionId, User requestingUser);

    /**
     * Obtiene todos los feedbacks de las atenciones de un practicante específico.
     * Implementa CU-010 (RF25, RF40) - Panel de supervisión para docentes.
     *
     * Este método permite a un supervisor (docente) revisar todo el feedback
     * recibido y emitido por sus practicantes a cargo.
     *
     * Validación de pertenencia:
     * - Solo supervisores que gestionen al practicante pueden acceder
     *
     * @param practitionerId ID del practicante
     * @param supervisorUser Usuario supervisor que solicita el feedback
     * @return Lista de feedbacks de todas las atenciones del practicante
     */
    List<Feedback> getFeedbackForPractitioner(Long practitionerId, User supervisorUser);

    /**
     * Obtiene un feedback específico por su ID.
     *
     * @param feedbackId ID del feedback
     * @return El feedback solicitado
     */
    Feedback getFeedbackById(Long feedbackId);
}
