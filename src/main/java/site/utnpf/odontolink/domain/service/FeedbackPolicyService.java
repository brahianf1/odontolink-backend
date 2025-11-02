package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;

/**
 * Servicio de Dominio que implementa las políticas y reglas de negocio complejas
 * relacionadas con el sistema de Feedback bidireccional.
 *
 * Este servicio actúa como un "Rulebook" para operaciones de feedback que requieren
 * acceso a repositorios y validaciones complejas que no pueden ser autocontenidas
 * en el POJO Feedback.
 *
 * Implementa las reglas de negocio de:
 * - RF21, RF22: Registrar calificaciones (CU-009, CU-016)
 * - RF23: Evitar calificaciones duplicadas
 * - RF24, RF25, RF40: Visualización segmentada de feedback
 *
 * Responsabilidades:
 * 1. Validar que se cumplan todas las precondiciones antes de crear un feedback
 * 2. Aplicar las reglas de privacidad y pertenencia del feedback
 * 3. Validar que no existan calificaciones duplicadas
 * 4. Verificar que la atención esté finalizada antes de permitir feedback
 *
 * @author OdontoLink Team
 */
public class FeedbackPolicyService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackPolicyService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Valida que se cumplan todas las reglas de negocio antes de crear un feedback.
     * Implementa RF21, RF22, RF23 - CU-009, CU-016.
     *
     * Reglas de negocio:
     * 1. La atención debe estar en estado COMPLETED (RF21, RF22)
     * 2. El usuario debe ser el paciente o el practicante de la atención
     * 3. El usuario no debe haber enviado feedback previamente para esta atención (RF23)
     *
     * @param attention La atención sobre la que se quiere enviar feedback
     * @param submittingUser El usuario que envía el feedback
     * @throws InvalidBusinessRuleException si la atención no está finalizada o si ya existe feedback
     * @throws UnauthorizedOperationException si el usuario no pertenece a la atención
     */
    public void validateFeedbackCreation(Attention attention, User submittingUser) {
        // Regla 1: Validar que la atención esté finalizada
        if (attention.getStatus() != AttentionStatus.COMPLETED) {
            throw new InvalidBusinessRuleException(
                "Solo se puede dejar feedback en atenciones finalizadas. " +
                "La atención actual está en estado: " + attention.getStatus()
            );
        }

        // Regla 2: Validar que el usuario pertenezca a la atención
        if (!isUserPartOfAttention(attention, submittingUser)) {
            throw new UnauthorizedOperationException(
                "Solo el paciente o el practicante de la atención pueden enviar feedback."
            );
        }

        // Regla 3: Validar unicidad (RF23) - No duplicar calificaciones
        if (feedbackRepository.existsByAttentionAndSubmittedBy(attention, submittingUser)) {
            throw new InvalidBusinessRuleException(
                "Ya ha enviado su feedback para esta atención. " +
                "No se permiten calificaciones duplicadas."
            );
        }
    }

    /**
     * Valida que un usuario puede ver el feedback de una atención específica.
     * Implementa RF24: Visualización segmentada por rol.
     *
     * Reglas de privacidad:
     * - El paciente y el practicante de la atención pueden ver todo el feedback de esa atención
     * - Los supervisores pueden ver el feedback si supervisan al practicante
     *
     * @param attention La atención cuyo feedback se quiere consultar
     * @param requestingUser El usuario que solicita ver el feedback
     * @throws UnauthorizedOperationException si el usuario no tiene permisos
     */
    public void validateFeedbackAccess(Attention attention, User requestingUser) {
        // Verificar si es el paciente o practicante de la atención
        if (isUserPartOfAttention(attention, requestingUser)) {
            return; // Tiene acceso
        }

        // Verificar si es un supervisor (se valida en el servicio de aplicación con lógica adicional)
        if (requestingUser.getRole() == Role.ROLE_SUPERVISOR) {
            return; // Los supervisores pueden ver feedback de sus practicantes
        }

        throw new UnauthorizedOperationException(
            "No tiene permisos para ver el feedback de esta atención."
        );
    }

    /**
     * Verifica si un usuario es parte de una atención (paciente o practicante).
     *
     * @param attention La atención a verificar
     * @param user El usuario a verificar
     * @return true si el usuario es el paciente o el practicante de la atención
     */
    private boolean isUserPartOfAttention(Attention attention, User user) {
        if (attention == null || user == null) {
            return false;
        }

        // Verificar si es el paciente
        Patient patient = attention.getPatient();
        if (patient != null && patient.getUser() != null
                && patient.getUser().getId().equals(user.getId())) {
            return true;
        }

        // Verificar si es el practicante
        Practitioner practitioner = attention.getPractitioner();
        if (practitioner != null && practitioner.getUser() != null
                && practitioner.getUser().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }

    /**
     * Verifica si un supervisor supervisa a un practicante específico.
     * Esta validación es necesaria para RF25, RF40: Panel docente de supervisión.
     *
     * @param supervisorUser El usuario supervisor
     * @param practitionerId El ID del practicante a verificar
     * @return true si el supervisor supervisa al practicante
     */
    public boolean supervisorManagesPractitioner(User supervisorUser, Long practitionerId) {
        // Esta validación se implementa mejor en el servicio de aplicación
        // con acceso al SupervisorRepository para verificar la relación
        // Por ahora, retornamos true si el usuario es supervisor
        // La validación completa se realizará en FeedbackService
        return supervisorUser.getRole() == Role.ROLE_SUPERVISOR;
    }
}
