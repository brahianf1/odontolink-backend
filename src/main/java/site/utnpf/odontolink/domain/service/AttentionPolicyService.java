package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.AttentionStatus;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de Dominio que implementa las políticas y reglas de negocio complejas
 * relacionadas con el ciclo de vida de las atenciones (casos clínicos).
 *
 * Este servicio actúa como un "Rulebook" para operaciones complejas que requieren
 * acceso a repositorios y no pueden ser autocontenidas en el POJO Attention.
 *
 * Implementa las reglas de negocio de:
 * - RF10, RF19: Finalizar Caso Clínico (CU 4.4)
 * - Funnel tracking: cierre lógico por abandono temprano cuando el último
 *   turno de una Atención queda CANCELLED o NO_SHOW.
 *
 * Responsabilidades:
 * 1. Validar que se cumplan todas las precondiciones antes de finalizar un caso
 * 2. Aplicar las reglas de negocio profesionales que requieren consultas a repositorios
 * 3. Delegar al POJO (Attention) las operaciones de cambio de estado simples
 *
 * @author OdontoLink Team
 */
public class AttentionPolicyService {

    private final AppointmentRepository appointmentRepository;
    private final AttentionRepository attentionRepository;

    public AttentionPolicyService(AppointmentRepository appointmentRepository,
                                  AttentionRepository attentionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.attentionRepository = attentionRepository;
    }

    /**
     * Finaliza un caso clínico aplicando todas las reglas de negocio profesionales.
     * Implementa RF10, RF19 - CU 4.4: Finalizar Caso Clínico.
     *
     * Reglas de negocio:
     * 1. El caso debe estar en estado IN_PROGRESS
     * 2. No deben existir turnos futuros agendados (SCHEDULED) para este caso
     * 3. Una vez finalizado, el caso cambia a COMPLETED y habilita el feedback
     *
     * El servicio de aplicación debe persistir el cambio después de esta operación.
     *
     * @param attention El caso clínico a finalizar
     * @throws InvalidBusinessRuleException si existen turnos futuros pendientes
     * @throws IllegalStateException si el caso no está en estado válido (delegado al POJO)
     */
    public void finalizeAttention(Attention attention) {
        // Validación 1: Verificar que no haya turnos futuros agendados
        boolean hasScheduledAppointments = hasScheduledFutureAppointments(attention.getId());

        if (hasScheduledAppointments) {
            throw new InvalidBusinessRuleException(
                "No se puede finalizar el caso clínico. " +
                "Aún existen turnos futuros agendados. " +
                "Por favor, complete o cancele todos los turnos pendientes antes de finalizar el caso."
            );
        }

        // Validación 2: Verificar que no haya turnos pendientes de marcación (SCHEDULED pasados)
        // Esta validación asegura que todos los turnos pasados fueron marcados como COMPLETED o NO_SHOW
        boolean hasPendingPastAppointments = hasPendingPastAppointments(attention.getId());

        if (hasPendingPastAppointments) {
            throw new InvalidBusinessRuleException(
                "No se puede finalizar el caso clínico. " +
                "Existen turnos pasados que aún no han sido marcados como completados o ausentes. " +
                "Por favor, revise el historial de turnos y marque la asistencia correspondiente."
            );
        }

        // Si todas las validaciones pasan, delegar al POJO para cambiar el estado
        // El POJO (Attention) se encarga de validar que el estado actual sea IN_PROGRESS
        attention.complete();
    }

    /**
     * Cancela manualmente una Atención por decisión del practicante responsable.
     *
     * Esta operación cubre el escenario que ni {@link #finalizeAttention} ni
     * {@link #closeAttentionIfAbandoned} alcanzan: un caso clínico con al menos
     * un turno COMPLETED del que el paciente no vuelve a aparecer. Sin esta
     * acción, la Atención quedaría {@link AttentionStatus#IN_PROGRESS}
     * indefinidamente, congelando cupo y manteniendo el chat abierto.
     *
     * Precondiciones (validadas acá porque requieren acceso a repositorio):
     * <ul>
     *   <li>No deben existir turnos {@link AppointmentStatus#SCHEDULED} a futuro
     *       para este caso. El practicante debe cancelarlos uno a uno antes
     *       (UX: el frontend lo guía). Política deliberada: evitar el borrado
     *       en cascada de turnos del paciente sin confirmación granular.</li>
     *   <li>No deben existir turnos SCHEDULED pasados sin marcar (los que el
     *       practicante todavía debe resolver como COMPLETED o NO_SHOW).
     *       Espeja la regla de {@link #finalizeAttention}: cualquier cierre
     *       del caso (sea por completar o por cancelar) exige que el destino
     *       de cada turno pasado esté declarado, para no dejar turnos huérfanos
     *       que distorsionan reportes y métricas.</li>
     * </ul>
     *
     * El POJO {@link Attention#cancelByPractitioner(String, User)} se encarga
     * del cambio de estado y del registro de auditoría como ProgressNote.
     *
     * @param attention Caso clínico cargado en memoria
     * @param motive Motivo de la cancelación (obligatorio)
     * @param author Usuario practicante autenticado
     * @throws InvalidBusinessRuleException si quedan turnos pendientes
     */
    public void cancelAttentionByPractitioner(Attention attention, String motive, User author) {
        if (hasScheduledFutureAppointments(attention.getId())) {
            throw new InvalidBusinessRuleException(
                    "No se puede cancelar el caso clínico mientras existan turnos a futuro " +
                    "agendados. Cancele primero los turnos pendientes y vuelva a intentar."
            );
        }
        if (hasPendingPastAppointments(attention.getId())) {
            throw new InvalidBusinessRuleException(
                    "No se puede cancelar el caso clínico mientras existan turnos pasados " +
                    "sin marcar. Marque cada turno como 'completado' o 'ausente' antes de " +
                    "cancelar el caso."
            );
        }
        attention.cancelByPractitioner(motive, author);
    }

    /**
     * Evalúa y, si corresponde, ejecuta el cierre lógico de una Atención por
     * abandono temprano (funnel tracking).
     *
     * Regla de negocio:
     * Tras cancelar un turno o marcarlo NO_SHOW, si la Atención padre se
     * queda SIN turnos SCHEDULED futuros y SIN turnos COMPLETED, significa
     * que el caso nunca produjo trabajo clínico efectivo y no hay próximos
     * turnos que recuperarlo. En ese escenario la Atención se cierra como
     * CANCELLED automáticamente.
     *
     * Decisiones de diseño:
     * - Si la atención ya está en COMPLETED o CANCELLED, la operación es un
     *   no-op silencioso. Esto es deliberado: el llamador (cancel del paciente,
     *   cancel del practicante, no-show) ya hizo lo suyo y no debe fallar
     *   porque una transición intermedia haya cerrado el caso antes.
     * - El cambio de estado se persiste con un UPDATE focalizado para no
     *   re-materializar el agregado completo.
     *
     * @param attentionId ID del caso clínico que acaba de perder un turno
     */
    public void closeAttentionIfAbandoned(Long attentionId) {
        if (attentionId == null) {
            return;
        }

        Optional<Attention> attentionOpt = attentionRepository.findById(attentionId);
        if (attentionOpt.isEmpty()) {
            return;
        }
        Attention attention = attentionOpt.get();

        // Solo aplica el cierre por abandono mientras la Atención esté abierta.
        if (attention.getStatus() != AttentionStatus.IN_PROGRESS) {
            return;
        }

        // Si todavía existe trabajo clínico efectivo (al menos un COMPLETED)
        // no se considera abandono: el caso debe permanecer abierto para
        // poder finalizarse o seguir recibiendo turnos.
        boolean hasAnyCompleted = appointmentRepository.existsByAttentionIdAndStatus(
                attentionId,
                AppointmentStatus.COMPLETED
        );
        if (hasAnyCompleted) {
            return;
        }

        // Si quedan turnos futuros agendados, el caso sigue vivo.
        // Se compara contra "ahora" para que un SCHEDULED ya pasado pero no
        // marcado no impida cerrar lo que objetivamente está abandonado.
        boolean hasFutureScheduled = appointmentRepository
                .existsByAttentionIdAndStatusAndAppointmentTimeGreaterThanEqual(
                        attentionId,
                        AppointmentStatus.SCHEDULED,
                        LocalDateTime.now()
                );
        if (hasFutureScheduled) {
            return;
        }

        // Sin trabajo realizado ni próximos turnos: la Atención está abandonada.
        // Persistimos el cierre con UPDATE atómico para no arrastrar el agregado completo.
        attentionRepository.updateStatus(attentionId, AttentionStatus.CANCELLED);
    }

    /**
     * Verifica si una atención tiene turnos agendados (SCHEDULED) en el futuro.
     *
     * @param attentionId ID del caso clínico
     * @return true si existen turnos futuros con estado SCHEDULED, false en caso contrario
     */
    private boolean hasScheduledFutureAppointments(Long attentionId) {
        // Buscar turnos con estado SCHEDULED asociados a esta atención
        // que tengan fecha/hora mayor o igual a ahora
        LocalDateTime now = LocalDateTime.now();
        return appointmentRepository.existsByAttentionIdAndStatusAndAppointmentTimeGreaterThanEqual(
            attentionId,
            AppointmentStatus.SCHEDULED,
            now
        );
    }

    /**
     * Verifica si una atención tiene turnos pasados que aún están en estado SCHEDULED.
     * Esto indica que el practicante no marcó la asistencia (COMPLETED o NO_SHOW).
     *
     * @param attentionId ID del caso clínico
     * @return true si existen turnos pasados sin marcar, false en caso contrario
     */
    private boolean hasPendingPastAppointments(Long attentionId) {
        // Buscar turnos con estado SCHEDULED asociados a esta atención
        // que tengan fecha/hora menor a ahora (están en el pasado)
        LocalDateTime now = LocalDateTime.now();
        return appointmentRepository.existsByAttentionIdAndStatusAndAppointmentTimeLessThan(
            attentionId,
            AppointmentStatus.SCHEDULED,
            now
        );
    }
}
