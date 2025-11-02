package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAttentionUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.ProgressNoteRepository;
import site.utnpf.odontolink.domain.service.AttentionPolicyService;

import java.util.List;

/**
 * Servicio de aplicación para la gestión de atenciones (casos clínicos).
 * Implementa el puerto de entrada IAttentionUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio es el orquestador transaccional de los casos de uso de la Fase 4:
 * - CU 4.2: Registrar Evolución (RF11)
 * - CU 4.4: Finalizar Caso Clínico (RF10, RF19)
 *
 * Su responsabilidad principal es coordinar:
 * 1. La carga de entidades de dominio desde los repositorios
 * 2. La validación de permisos y autorización
 * 3. La delegación de lógica de negocio al servicio de dominio o a los POJOs
 * 4. La persistencia transaccional de los cambios
 *
 * Flujo de ejecución típico:
 * Controller -> AttentionService (aquí) -> AttentionPolicyService (dominio) -> Repositories
 *
 * @Transactional asegura que toda la operación sea atómica.
 *
 * @author OdontoLink Team
 */
@Transactional
public class AttentionService implements IAttentionUseCase {

    private final AttentionRepository attentionRepository;
    private final ProgressNoteRepository progressNoteRepository;
    private final AttentionPolicyService attentionPolicyService;

    public AttentionService(
            AttentionRepository attentionRepository,
            ProgressNoteRepository progressNoteRepository,
            AttentionPolicyService attentionPolicyService) {
        this.attentionRepository = attentionRepository;
        this.progressNoteRepository = progressNoteRepository;
        this.attentionPolicyService = attentionPolicyService;
    }

    /**
     * Implementa el caso de uso CU 4.2: "Registrar Evolución".
     *
     * Orquestación:
     * 1. Busca la Attention desde el repositorio
     * 2. Valida que el usuario tenga permisos (es el practicante o un supervisor)
     * 3. Delega al POJO (Attention) para añadir la nota de progreso
     * 4. Persiste la Attention (y su ProgressNote hija gracias a CascadeType.ALL) de forma transaccional
     *
     * @param attentionId ID del caso clínico
     * @param noteContent Contenido de la nota de evolución
     * @param authorUser Usuario que registra la evolución
     * @return La Attention actualizada con la nueva ProgressNote
     * @throws ResourceNotFoundException si la atención no existe
     * @throws IllegalStateException si el caso no está en estado válido (delegado al POJO)
     * @throws IllegalArgumentException si el autor no tiene permisos (delegado al POJO)
     */
    @Override
    public Attention addProgressNoteToAttention(Long attentionId, String noteContent, User authorUser) {
        // Cargar la Attention desde el repositorio
        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attention", "id", attentionId.toString()));

        // Delegar al POJO (Attention) para aplicar las reglas de negocio
        // El POJO se encarga de:
        // - Validar que el caso esté en estado IN_PROGRESS
        // - Validar que el autor sea el practicante o un supervisor
        // - Crear y añadir la ProgressNote
        attention.addProgressNote(noteContent, authorUser);

        // Persistir la Attention (y su ProgressNote hija gracias a CascadeType.ALL)
        // Esta es la operación transaccional que garantiza atomicidad
        return attentionRepository.save(attention);
    }

    /**
     * Implementa el caso de uso CU 4.4: "Finalizar Caso Clínico".
     *
     * Orquestación:
     * 1. Busca la Attention desde el repositorio
     * 2. Valida que el usuario tenga permisos (es el practicante responsable)
     * 3. Delega al servicio de dominio (AttentionPolicyService) para aplicar las reglas complejas
     * 4. Persiste la Attention de forma transaccional
     *
     * @param attentionId ID del caso clínico a finalizar
     * @param practitionerUser Usuario practicante
     * @return La Attention actualizada con estado COMPLETED
     * @throws ResourceNotFoundException si la atención no existe
     * @throws UnauthorizedOperationException si el usuario no es el practicante responsable
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException si hay turnos pendientes (delegado al servicio de dominio)
     */
    @Override
    public Attention finalizeAttention(Long attentionId, User practitionerUser) {
        // Cargar la Attention desde el repositorio
        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attention", "id", attentionId.toString()));

        // Validar pertenencia: Solo el practicante responsable puede finalizar su caso
        // Comparar el User del Practitioner con el User autenticado
        Practitioner practitioner = attention.getPractitioner();
        if (practitioner == null || practitioner.getUser() == null
                || !practitioner.getUser().getId().equals(practitionerUser.getId())) {
            throw new UnauthorizedOperationException(
                "Solo el practicante responsable del caso puede finalizarlo."
            );
        }

        // Delegar al servicio de dominio (AttentionPolicyService) para aplicar todas las reglas de negocio complejas
        // El servicio de dominio se encarga de:
        // - Validar que no haya turnos futuros agendados
        // - Validar que no haya turnos pasados sin marcar
        // - Delegar al POJO (Attention) para cambiar el estado a COMPLETED
        attentionPolicyService.finalizeAttention(attention);

        // Persistir la Attention con el nuevo estado
        return attentionRepository.save(attention);
    }

    /**
     * Obtiene un caso clínico específico por su ID.
     *
     * @param attentionId ID del caso clínico
     * @return La Attention solicitada
     * @throws ResourceNotFoundException si la atención no existe
     */
    @Override
    @Transactional(readOnly = true)
    public Attention getAttentionById(Long attentionId) {
        return attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attention", "id", attentionId.toString()));
    }

    /**
     * Obtiene todos los casos clínicos de un practicante específico.
     *
     * @param practitionerId ID del practicante
     * @return Lista de atenciones del practicante
     */
    @Override
    @Transactional(readOnly = true)
    public List<Attention> getAttentionsByPractitioner(Long practitionerId) {
        return attentionRepository.findByPractitionerId(practitionerId);
    }

    /**
     * Obtiene todos los casos clínicos de un paciente específico.
     *
     * @param patientId ID del paciente
     * @return Lista de atenciones del paciente
     */
    @Override
    @Transactional(readOnly = true)
    public List<Attention> getAttentionsByPatient(Long patientId) {
        return attentionRepository.findByPatientId(patientId);
    }

    /**
     * Obtiene las notas de progreso de un caso clínico específico.
     *
     * @param attentionId ID del caso clínico
     * @return Lista de notas de progreso del caso
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProgressNote> getProgressNotesByAttention(Long attentionId) {
        // Verificar que la atención existe
        if (!attentionRepository.findById(attentionId).isPresent()) {
            throw new ResourceNotFoundException("Attention", "id", attentionId.toString());
        }

        return progressNoteRepository.findByAttentionId(attentionId);
    }
}
