package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada para el Módulo Académico de Auditoría Clínica del Supervisor (RF39).
 *
 * Este caso de uso modela la potestad académica del Docente para auditar y, cuando
 * corresponda, cerrar el expediente clínico (Atención) de los Practicantes vinculados
 * a su lista de "my-practitioners".
 *
 * Filosofía de diseño:
 * - Separación de responsabilidades respecto de {@link ISupervisorUseCase} (que gobierna
 *   la vinculación N-a-N) y de {@link IAttentionUseCase} (que gobierna el flujo desde la
 *   perspectiva del propio Practicante).
 * - Todos los métodos comparten un mismo "cerco de seguridad": el supervisor SOLO opera
 *   sobre atenciones de practicantes ACTUALMENTE vinculados a él.
 *
 * Implementa RF39 - Auditoría y supervisión de Atenciones por parte del Docente.
 */
public interface ISupervisorAttentionUseCase {

    /**
     * Lista todas las atenciones (expedientes clínicos) de un practicante vinculado.
     *
     * Reglas:
     * - El supervisor debe existir.
     * - El practicante debe existir y estar vinculado al supervisor autenticado.
     *
     * @param practitionerId ID del practicante cuyo expediente se audita
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @return Lista de atenciones del practicante (puede estar vacía)
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException
     *         si el supervisor o el practicante no existen
     * @throws site.utnpf.odontolink.domain.exception.UnauthorizedOperationException
     *         si el practicante NO está vinculado al supervisor autenticado
     */
    List<Attention> getPractitionerAttentions(Long practitionerId, User supervisorUser);

    /**
     * Devuelve el detalle de una atención específica de un practicante vinculado.
     *
     * Reglas (cerco de seguridad RF39):
     * - El supervisor debe existir.
     * - El practicante debe existir y estar vinculado al supervisor autenticado.
     * - La atención debe existir y pertenecer al practicante indicado en el path
     *   (evita "URL tampering" o cross-tenant access).
     *
     * @param practitionerId ID del practicante propietario de la atención
     * @param attentionId    ID de la atención a auditar
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @return La atención solicitada (con turnos y notas materializadas)
     */
    Attention getPractitionerAttentionDetail(Long practitionerId, Long attentionId, User supervisorUser);

    /**
     * Obtiene las notas de progreso (evolución clínica) de una atención de un practicante vinculado.
     *
     * Aplica el mismo cerco de seguridad que el detalle: vínculo + coherencia paciente↔atención.
     *
     * @param practitionerId ID del practicante propietario de la atención
     * @param attentionId    ID de la atención a auditar
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @return Lista de notas de progreso del caso (puede estar vacía)
     */
    List<ProgressNote> getPractitionerAttentionProgressNotes(Long practitionerId,
                                                             Long attentionId,
                                                             User supervisorUser);

    /**
     * Finaliza por autoridad académica una atención de un practicante vinculado.
     *
     * Esta es la operación de "cierre académico de la carpeta" (RF39 - Escritura):
     * el Docente revisa el caso, aprueba el trabajo del alumno y lo lleva a COMPLETED.
     * También cubre el caso de que el alumno olvide cerrarlo.
     *
     * Reglas:
     * - Cerco de seguridad (mismo que las lecturas).
     * - Se reutilizan las reglas clínicas del {@link site.utnpf.odontolink.domain.service.AttentionPolicyService}:
     *   no debe haber turnos futuros SCHEDULED ni turnos pasados sin marcar.
     * - El POJO {@link Attention#complete()} valida que el estado actual sea IN_PROGRESS.
     *
     * @param practitionerId ID del practicante dueño del expediente
     * @param attentionId    ID de la atención a cerrar
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @return La atención con estado COMPLETED
     */
    Attention finalizeAttentionAsSupervisor(Long practitionerId, Long attentionId, User supervisorUser);
}
