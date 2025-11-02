package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;

import java.time.LocalDateTime;

/**
 * Servicio de Dominio que implementa las políticas y reglas de negocio complejas
 * relacionadas con el ciclo de vida de las atenciones (casos clínicos).
 *
 * Este servicio actúa como un "Rulebook" para operaciones complejas que requieren
 * acceso a repositorios y no pueden ser autocontenidas en el POJO Attention.
 *
 * Implementa las reglas de negocio de:
 * - RF10, RF19: Finalizar Caso Clínico (CU 4.4)
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

    public AttentionPolicyService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
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
        boolean hasScheduledAppointments = hasScheduledAppointments(attention.getId());

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
     * Verifica si una atención tiene turnos agendados (SCHEDULED) en el futuro.
     *
     * @param attentionId ID del caso clínico
     * @return true si existen turnos futuros con estado SCHEDULED, false en caso contrario
     */
    private boolean hasScheduledAppointments(Long attentionId) {
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
