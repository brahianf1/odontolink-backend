package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IFeedbackUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.service.FeedbackPolicyService;

import java.util.List;

/**
 * Servicio de aplicación para la gestión de Feedback.
 * Implementa el puerto de entrada IFeedbackUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio es el orquestador transaccional de los casos de uso:
 * - CU-009: Calificar Paciente (RF21)
 * - CU-016: Calificar Practicante (RF22)
 * - CU-010: Visualizar Feedback (RF24, RF25, RF40)
 *
 * Su responsabilidad principal es coordinar:
 * 1. La carga de entidades de dominio desde los repositorios
 * 2. La validación de permisos y autorización
 * 3. La delegación de lógica de negocio al servicio de dominio (FeedbackPolicyService)
 * 4. La persistencia transaccional de los cambios
 *
 * Flujo de ejecución típico:
 * Controller -> FeedbackService (aquí) -> FeedbackPolicyService (dominio) -> Repositories
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
    private final SupervisorRepository supervisorRepository;
    private final PractitionerRepository practitionerRepository;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            AttentionRepository attentionRepository,
            FeedbackPolicyService feedbackPolicyService,
            SupervisorRepository supervisorRepository,
            PractitionerRepository practitionerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.attentionRepository = attentionRepository;
        this.feedbackPolicyService = feedbackPolicyService;
        this.supervisorRepository = supervisorRepository;
        this.practitionerRepository = practitionerRepository;
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
     * Implementa el caso de uso CU-010: "Visualizar Feedback de un Practicante".
     * Para supervisores (docentes) - RF25, RF40.
     *
     * Orquestación:
     * 1. Valida que el usuario sea un supervisor
     * 2. Valida que el supervisor gestione al practicante (relación de supervisión)
     * 3. Retorna todos los feedbacks de las atenciones del practicante
     *
     * @param practitionerId ID del practicante
     * @param supervisorUser Usuario supervisor que solicita el feedback
     * @return Lista de feedbacks de todas las atenciones del practicante
     * @throws UnauthorizedOperationException si el usuario no es supervisor o no gestiona al practicante
     * @throws ResourceNotFoundException si el practicante no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getFeedbackForPractitioner(Long practitionerId, User supervisorUser) {
        // Validar que el usuario sea un supervisor
        if (supervisorUser.getRole() != Role.ROLE_SUPERVISOR) {
            throw new UnauthorizedOperationException(
                "Solo los supervisores pueden acceder al feedback de practicantes."
            );
        }

        // Verificar que el practicante existe
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "id", practitionerId.toString()));

        // Validar que el supervisor gestione al practicante
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "userId", supervisorUser.getId().toString()));

        // Verificar la relación de supervisión
        if (!supervisorManagesPractitioner(supervisor, practitioner)) {
            throw new UnauthorizedOperationException(
                "No tiene permisos para acceder al feedback de este practicante. " +
                "Solo puede ver el feedback de los practicantes a su cargo."
            );
        }

        // Retornar todos los feedbacks de las atenciones del practicante
        return feedbackRepository.findByPractitionerId(practitionerId);
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

    /**
     * Verifica si un supervisor gestiona a un practicante específico.
     *
     * @param supervisor El supervisor
     * @param practitioner El practicante
     * @return true si el supervisor gestiona al practicante
     */
    private boolean supervisorManagesPractitioner(Supervisor supervisor, Practitioner practitioner) {
        // Verificar si el practicante está en el conjunto de practicantes supervisados
        if (supervisor.getSupervisedPractitioners() == null) {
            return false;
        }
        return supervisor.getSupervisedPractitioners().stream()
                .anyMatch(p -> p.getId().equals(practitioner.getId()));
    }
}
