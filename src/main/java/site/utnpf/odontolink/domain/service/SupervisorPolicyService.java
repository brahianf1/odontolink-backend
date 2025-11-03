package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;

import java.util.HashSet;

/**
 * Servicio de Dominio que implementa las políticas y reglas de negocio
 * relacionadas con la vinculación académica entre Supervisores y Practicantes.
 *
 * Este servicio actúa como un "Rulebook" para las operaciones de supervisión,
 * gestionando la relación N-a-N entre supervisores y practicantes siguiendo
 * el principio de desacoplamiento entre el contexto clínico y el contexto académico.
 *
 * Implementa las reglas de negocio de:
 * - RF22, RF37: Vincular Practicantes (CU 7.1)
 * - Desvincular Practicantes (CU 7.2)
 * - RF40: Validación de acceso del supervisor al feedback de practicantes
 *
 * Filosofía de Diseño:
 * - El supervisor actúa como "observador" del contexto clínico
 * - La vinculación es un prerrequisito para todas las funcionalidades de supervisión
 * - El flujo clínico (Attention, Appointment) funciona de forma autónoma
 * - La relación N-a-N es bidireccional y debe mantenerse consistente
 *
 * @author OdontoLink Team
 */
public class SupervisorPolicyService {

    /**
     * Vincula un practicante a un supervisor, estableciendo una relación de supervisión académica.
     * Implementa RF22, RF37 - CU 7.1: Vincular Practicante.
     *
     * Reglas de negocio:
     * 1. El vínculo debe ser bidireccional (consistencia del modelo N-a-N)
     * 2. No se permiten vínculos duplicados
     * 3. Los conjuntos de relaciones se inicializan si son nulos
     *
     * Esta operación no persiste los cambios. El servicio de aplicación
     * debe guardar las entidades después de llamar a este método.
     *
     * @param supervisor El supervisor que vincula al practicante
     * @param practitioner El practicante a vincular
     * @throws InvalidBusinessRuleException si el vínculo ya existe
     */
    public void linkPractitioner(Supervisor supervisor, Practitioner practitioner) {
        if (supervisor == null || practitioner == null) {
            throw new IllegalArgumentException("El supervisor y el practicante no pueden ser nulos.");
        }

        // Inicializar conjuntos si son nulos (lazy loading)
        if (supervisor.getSupervisedPractitioners() == null) {
            supervisor.setSupervisedPractitioners(new HashSet<>());
        }
        if (practitioner.getSupervisors() == null) {
            practitioner.setSupervisors(new HashSet<>());
        }

        // Validar que no exista un vínculo previo
        if (supervisor.getSupervisedPractitioners().contains(practitioner)) {
            throw new InvalidBusinessRuleException(
                "El practicante ya está vinculado a este supervisor. " +
                "No se permiten vínculos duplicados."
            );
        }

        // Establecer el vínculo bidireccional
        supervisor.getSupervisedPractitioners().add(practitioner);
        practitioner.getSupervisors().add(supervisor);
    }

    /**
     * Desvincula un practicante de un supervisor, removiendo la relación de supervisión académica.
     * Implementa CU 7.2: Desvincular Practicante.
     *
     * Reglas de negocio:
     * 1. El vínculo debe existir previamente
     * 2. La desvinculación debe ser bidireccional (consistencia del modelo N-a-N)
     *
     * Esta operación no persiste los cambios. El servicio de aplicación
     * debe guardar las entidades después de llamar a este método.
     *
     * @param supervisor El supervisor que desvincula al practicante
     * @param practitioner El practicante a desvincular
     * @throws InvalidBusinessRuleException si el vínculo no existe
     */
    public void unlinkPractitioner(Supervisor supervisor, Practitioner practitioner) {
        if (supervisor == null || practitioner == null) {
            throw new IllegalArgumentException("El supervisor y el practicante no pueden ser nulos.");
        }

        // Validar que exista el vínculo
        if (supervisor.getSupervisedPractitioners() == null ||
            !supervisor.getSupervisedPractitioners().contains(practitioner)) {
            throw new InvalidBusinessRuleException(
                "El practicante no está vinculado a este supervisor. " +
                "No se puede desvincular una relación inexistente."
            );
        }

        // Romper el vínculo bidireccional
        supervisor.getSupervisedPractitioners().remove(practitioner);
        if (practitioner.getSupervisors() != null) {
            practitioner.getSupervisors().remove(supervisor);
        }
    }

    /**
     * Valida que un supervisor tiene permisos para acceder a información de un practicante.
     * Implementa RF40: Acceso del supervisor al feedback y atenciones de practicantes.
     *
     * Esta validación es fundamental para asegurar que solo supervisores con vínculos
     * activos puedan acceder a información académica y clínica de sus practicantes.
     *
     * Reglas de negocio:
     * 1. El usuario debe tener rol SUPERVISOR
     * 2. Debe existir una relación de supervisión activa con el practicante
     *
     * @param supervisorUser El usuario con rol supervisor
     * @param practitionerId El ID del practicante a acceder
     * @param supervisor El supervisor asociado al usuario
     * @param practitioner El practicante a verificar
     * @throws UnauthorizedOperationException si no existe vínculo de supervisión
     */
    public void validateSupervisorAccess(User supervisorUser, Long practitionerId,
                                        Supervisor supervisor, Practitioner practitioner) {
        if (supervisorUser == null || supervisor == null || practitioner == null) {
            throw new IllegalArgumentException("Los parámetros de validación no pueden ser nulos.");
        }

        // Validar que existe el vínculo de supervisión
        if (supervisor.getSupervisedPractitioners() == null ||
            !supervisor.getSupervisedPractitioners().contains(practitioner)) {
            throw new UnauthorizedOperationException(
                "Acceso denegado. Usted no supervisa a este practicante. " +
                "Solo puede acceder a la información de los practicantes a su cargo."
            );
        }
    }

    /**
     * Verifica si un supervisor gestiona a un practicante específico.
     * Método de utilidad para validaciones rápidas de relación de supervisión.
     *
     * @param supervisor El supervisor
     * @param practitioner El practicante
     * @return true si existe vínculo de supervisión, false en caso contrario
     */
    public boolean supervisorManagesPractitioner(Supervisor supervisor, Practitioner practitioner) {
        if (supervisor == null || practitioner == null) {
            return false;
        }

        if (supervisor.getSupervisedPractitioners() == null) {
            return false;
        }

        return supervisor.getSupervisedPractitioners().contains(practitioner);
    }
}
