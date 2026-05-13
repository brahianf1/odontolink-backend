package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IFeedbackUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.service.FeedbackPolicyService;

import java.util.List;

/**
 * Servicio de aplicación para la gestión MICRO-contexto de Feedback (atención puntual).
 * Implementa el puerto de entrada IFeedbackUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Casos de uso orquestados:
 *  - CU-009: Calificar Paciente (RF21)
 *  - CU-016: Calificar Practicante (RF22)
 *  - CU-010: Visualizar Feedback de una atención (RF24)
 *
 * El MACRO-contexto del docente (Panel de Supervisión RF25) vive en
 * {@code SupervisorFeedbackDashboardService}. Mantener ambos servicios
 * separados respeta Responsabilidad Única y mantiene este servicio libre
 * de dependencias analíticas (paginación, agregados, cerco multi-alumno).
 *
 * Responsabilidades:
 *  1. Cargar entidades de dominio desde los repositorios.
 *  2. Validar permisos y autorización.
 *  3. Delegar la lógica de negocio al servicio de dominio (FeedbackPolicyService).
 *  4. Persistir transaccionalmente los cambios.
 *
 * @Transactional asegura que toda la operación sea atómica.
 *
 * @author OdontoLink Team
 */
@Transactional
public class FeedbackService implements IFeedbackUseCase {

    private final FeedbackRepository feedbackRepository;
    private final AttentionRepository attentionRepository;
    private final FeedbackPolicyService feedbackPolicyService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           AttentionRepository attentionRepository,
                           FeedbackPolicyService feedbackPolicyService) {
        this.feedbackRepository = feedbackRepository;
        this.attentionRepository = attentionRepository;
        this.feedbackPolicyService = feedbackPolicyService;
    }

    /**
     * Implementa el caso de uso CU-009, CU-016: "Crear Feedback".
     *
     * Orquestación:
     * 1. Busca la Attention desde el repositorio
     * 2. Delega al servicio de dominio (FeedbackPolicyService) para validar las reglas de negocio
     * 3. Crea el nuevo POJO Feedback
     * 4. Persiste el Feedback de forma transaccional
     *
     * @param attentionId ID de la atención sobre la que se envía el feedback
     * @param rating Calificación (1-5 estrellas)
     * @param comment Comentario opcional
     * @param submittingUser Usuario que envía el feedback
     * @return El feedback creado
     * @throws ResourceNotFoundException si la atención no existe
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException si la atención no está finalizada o ya existe feedback
     * @throws UnauthorizedOperationException si el usuario no pertenece a la atención
     */
    @Override
    public Feedback createFeedback(Long attentionId, int rating, String comment, User submittingUser) {
        // Cargar la Attention desde el repositorio
        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attention", "id", attentionId.toString()));

        // Delegar al servicio de dominio (FeedbackPolicyService) para validar todas las reglas de negocio
        // El servicio de dominio se encarga de:
        // - Validar que la atención esté COMPLETED (RF21, RF22)
        // - Validar que el usuario sea el paciente o practicante
        // - Validar que no exista feedback previo (RF23)
        feedbackPolicyService.validateFeedbackCreation(attention, submittingUser);

        // Crear el nuevo POJO Feedback
        // El constructor del POJO valida el rating (1-5)
        Feedback newFeedback = new Feedback(attention, submittingUser, rating, comment);

        // Persistir el Feedback
        return feedbackRepository.save(newFeedback);
    }

    /**
     * Implementa el caso de uso CU-010: "Visualizar Feedback de una Atención".
     * Para pacientes y practicantes (RF24).
     *
     * Orquestación:
     * 1. Busca la Attention desde el repositorio
     * 2. Valida que el usuario tenga permisos para ver el feedback
     * 3. Retorna todos los feedbacks de la atención
     *
     * @param attentionId ID de la atención
     * @param requestingUser Usuario que solicita ver el feedback
     * @return Lista de feedbacks de la atención
     * @throws ResourceNotFoundException si la atención no existe
     * @throws UnauthorizedOperationException si el usuario no tiene permisos
     */
    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getFeedbackForAttention(Long attentionId, User requestingUser) {
        // Cargar la Attention desde el repositorio
        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attention", "id", attentionId.toString()));

        // Validar permisos de acceso
        // El servicio de dominio verifica que el usuario sea paciente, practicante o supervisor
        feedbackPolicyService.validateFeedbackAccess(attention, requestingUser);

        // Retornar todos los feedbacks de la atención
        return feedbackRepository.findByAttention(attention);
    }

    /**
     * Obtiene un feedback específico por su ID.
     *
     * @param feedbackId ID del feedback
     * @return El feedback solicitado
     * @throws ResourceNotFoundException si el feedback no existe
     */
    @Override
    @Transactional(readOnly = true)
    public Feedback getFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", feedbackId.toString()));
    }
}
