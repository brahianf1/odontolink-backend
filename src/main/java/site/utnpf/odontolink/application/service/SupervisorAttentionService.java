package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISupervisorAttentionUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.ProgressNoteRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.service.AttentionPolicyService;
import site.utnpf.odontolink.domain.service.SupervisorPolicyService;

import java.util.List;

/**
 * Servicio de aplicación que implementa RF39 - Auditoría y Supervisión de Atenciones.
 *
 * Es el orquestador transaccional de los casos de uso académicos del Docente sobre
 * el expediente clínico del Practicante:
 *  - Listar atenciones de un practicante vinculado
 *  - Auditar el detalle de una atención (turnos + evoluciones)
 *  - Listar notas de progreso de una atención
 *  - Finalizar por autoridad académica una atención (override del docente)
 *
 * Decisiones arquitectónicas:
 *  - El "cerco de seguridad" se centraliza reutilizando {@link SupervisorPolicyService#validateSupervisorAccess}
 *    para evitar duplicar la regla "el supervisor solo opera sobre sus practicantes".
 *  - La finalización reutiliza {@link AttentionPolicyService#finalizeAttention} para no duplicar
 *    las reglas clínicas (no hay turnos futuros pendientes ni turnos pasados sin marcar).
 *  - Se aplica defensa en profundidad: además del vínculo docente-alumno, se valida que la atención
 *    realmente pertenezca al practicante indicado en el path (evita acceso cruzado por URL).
 */
@Transactional
public class SupervisorAttentionService implements ISupervisorAttentionUseCase {

    private final SupervisorRepository supervisorRepository;
    private final PractitionerRepository practitionerRepository;
    private final AttentionRepository attentionRepository;
    private final ProgressNoteRepository progressNoteRepository;
    private final SupervisorPolicyService supervisorPolicyService;
    private final AttentionPolicyService attentionPolicyService;

    public SupervisorAttentionService(SupervisorRepository supervisorRepository,
                                      PractitionerRepository practitionerRepository,
                                      AttentionRepository attentionRepository,
                                      ProgressNoteRepository progressNoteRepository,
                                      SupervisorPolicyService supervisorPolicyService,
                                      AttentionPolicyService attentionPolicyService) {
        this.supervisorRepository = supervisorRepository;
        this.practitionerRepository = practitionerRepository;
        this.attentionRepository = attentionRepository;
        this.progressNoteRepository = progressNoteRepository;
        this.supervisorPolicyService = supervisorPolicyService;
        this.attentionPolicyService = attentionPolicyService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attention> getPractitionerAttentions(Long practitionerId, User supervisorUser) {
        // Aplicamos el cerco docente-alumno antes de cualquier lectura del expediente.
        Practitioner practitioner = enforceSupervisionBond(practitionerId, supervisorUser);

        // Una vez verificado el vínculo, listamos las atenciones del practicante.
        return attentionRepository.findByPractitionerId(practitioner.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Attention getPractitionerAttentionDetail(Long practitionerId, Long attentionId, User supervisorUser) {
        // Validamos vínculo y cargamos la atención asegurando la coherencia practicante↔atención.
        enforceSupervisionBond(practitionerId, supervisorUser);
        return loadAttentionOfPractitioner(attentionId, practitionerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressNote> getPractitionerAttentionProgressNotes(Long practitionerId,
                                                                    Long attentionId,
                                                                    User supervisorUser) {
        // Cerco + coherencia: el caso debe existir y pertenecer al practicante del path.
        enforceSupervisionBond(practitionerId, supervisorUser);
        loadAttentionOfPractitioner(attentionId, practitionerId);

        return progressNoteRepository.findByAttentionId(attentionId);
    }

    @Override
    public Attention finalizeAttentionAsSupervisor(Long practitionerId, Long attentionId, User supervisorUser) {
        // Validamos el vínculo docente-alumno antes de cualquier modificación.
        enforceSupervisionBond(practitionerId, supervisorUser);

        // Cargamos asegurando que la atención sea efectivamente del practicante del path.
        Attention attention = loadAttentionOfPractitioner(attentionId, practitionerId);

        // Reutilizamos el "Rulebook" clínico ya existente: aplica las mismas validaciones
        // que para el practicante (sin turnos futuros, sin pasados pendientes) y delega
        // el cambio de estado al POJO Attention.complete(). El supervisor opera con la
        // misma higiene clínica que el alumno; lo único que cambia es la autorización.
        attentionPolicyService.finalizeAttention(attention);

        // Persistimos el cambio de estado.
        return attentionRepository.save(attention);
    }

    /**
     * Aplica el cerco de seguridad RF39: el usuario autenticado debe tener rol SUPERVISOR,
     * existir como Supervisor en BD, y tener al practicante objetivo vinculado.
     *
     * Devuelve el Practitioner cargado para su reutilización aguas abajo.
     */
    private Practitioner enforceSupervisionBond(Long practitionerId, User supervisorUser) {
        // Defensa adicional al @PreAuthorize: si por algún motivo cambia la anotación,
        // el dominio sigue rechazando a quien no sea supervisor.
        if (supervisorUser == null || supervisorUser.getRole() != Role.ROLE_SUPERVISOR) {
            throw new UnauthorizedOperationException(
                "Solo los supervisores pueden auditar atenciones de practicantes."
            );
        }

        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supervisor", "userId", supervisorUser.getId().toString()));

        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Practitioner", "id", practitionerId.toString()));

        // Centralizamos la validación de vínculo en el servicio de dominio existente.
        supervisorPolicyService.validateSupervisorAccess(supervisorUser, practitionerId, supervisor, practitioner);

        return practitioner;
    }

    /**
     * Carga la atención y verifica que pertenezca al practicante indicado.
     * Evita que un supervisor (con vínculo al practicante A) pueda leer una atención
     * del practicante B simplemente cambiando el ID en la URL.
     */
    private Attention loadAttentionOfPractitioner(Long attentionId, Long practitionerId) {
        Attention attention = attentionRepository.findById(attentionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attention", "id", attentionId.toString()));

        if (attention.getPractitioner() == null
                || attention.getPractitioner().getId() == null
                || !attention.getPractitioner().getId().equals(practitionerId)) {
            // No filtramos detalles del propietario real para no leakear información cruzada.
            throw new UnauthorizedOperationException(
                "La atención solicitada no pertenece al practicante indicado."
            );
        }

        return attention;
    }
}
