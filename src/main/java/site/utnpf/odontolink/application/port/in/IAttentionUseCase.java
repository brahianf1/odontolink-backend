package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada (Input Port) para casos de uso relacionados con atenciones (casos clínicos).
 * Define las operaciones de trazabilidad del caso clínico según la Fase 4.
 *
 * Este puerto implementa los siguientes requisitos funcionales:
 * - RF11: Registrar evolución del paciente (CU 4.2)
 * - RF10, RF19: Finalizar caso clínico (CU 4.4)
 *
 * Este puerto trabaja exclusivamente con objetos de dominio, manteniendo la capa de aplicación
 * independiente de la infraestructura. La conversión de DTOs a objetos de dominio se realiza
 * en los adaptadores de entrada (controladores con ayuda de mappers).
 *
 * @author OdontoLink Team
 */
public interface IAttentionUseCase {

    /**
     * Añade una nota de progreso (evolución) al caso clínico de un paciente.
     * Implementa RF11 - CU 4.2: Registrar Evolución.
     *
     * Este método orquesta:
     * 1. La carga de la Attention desde el repositorio
     * 2. La validación de permisos (el author debe ser el practicante o un supervisor)
     * 3. La delegación al POJO (Attention) para añadir la nota
     * 4. La persistencia transaccional del cambio
     *
     * Validaciones:
     * - La atención debe existir
     * - La atención debe estar en estado IN_PROGRESS
     * - El autor debe ser el practicante responsable o un supervisor
     *
     * @param attentionId ID del caso clínico al que se añade la nota
     * @param noteContent Contenido de la nota de evolución
     * @param authorUser Usuario que registra la evolución (obtenido del contexto de seguridad)
     * @return La Attention actualizada con la nueva ProgressNote
     */
    Attention addProgressNoteToAttention(Long attentionId, String noteContent, User authorUser);

    /**
     * Finaliza un caso clínico completo, cambiando su estado a COMPLETED.
     * Implementa RF10, RF19 - CU 4.4: Finalizar Caso Clínico.
     *
     * Este método orquesta:
     * 1. La carga de la Attention desde el repositorio
     * 2. La validación de permisos (el usuario debe ser el practicante responsable)
     * 3. La delegación al servicio de dominio (AttentionPolicyService) para aplicar las reglas complejas
     * 4. La persistencia transaccional del cambio de estado
     *
     * Validaciones (aplicadas por AttentionPolicyService):
     * - La atención debe existir
     * - La atención debe estar en estado IN_PROGRESS
     * - No deben existir turnos futuros agendados (SCHEDULED)
     * - Todos los turnos pasados deben estar marcados (COMPLETED o NO_SHOW)
     *
     * Efecto: Al finalizar el caso, se habilita la funcionalidad de feedback (RF21, RF22)
     *
     * @param attentionId ID del caso clínico a finalizar
     * @param practitionerUser Usuario practicante que finaliza el caso (obtenido del contexto de seguridad)
     * @return La Attention actualizada con estado COMPLETED
     */
    Attention finalizeAttention(Long attentionId, User practitionerUser);

    /**
     * Obtiene un caso clínico específico por su ID.
     * Permite al practicante consultar los detalles de un caso.
     *
     * @param attentionId ID del caso clínico
     * @return La Attention solicitada
     */
    Attention getAttentionById(Long attentionId);

    /**
     * Obtiene todos los casos clínicos de un practicante específico.
     * Permite al practicante ver su lista de casos (activos y finalizados).
     *
     * @param practitionerId ID del practicante
     * @return Lista de atenciones del practicante
     */
    List<Attention> getAttentionsByPractitioner(Long practitionerId);

    /**
     * Obtiene todos los casos clínicos de un paciente específico.
     * Permite al paciente ver su historial de atenciones.
     *
     * @param patientId ID del paciente
     * @return Lista de atenciones del paciente
     */
    List<Attention> getAttentionsByPatient(Long patientId);

    /**
     * Obtiene las notas de progreso de un caso clínico específico.
     * Permite consultar el historial de evoluciones de un caso.
     *
     * @param attentionId ID del caso clínico
     * @return Lista de notas de progreso del caso
     */
    List<ProgressNote> getProgressNotesByAttention(Long attentionId);
}
