package site.utnpf.odontolink.application.service.support;

import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centraliza la resolución del cerco docente→practicantes que comparten el
 * Panel Docente de Feedback (RF25) y los charts del PO. Antes existía
 * duplicado dentro de {@code SupervisorFeedbackDashboardService}: extraído
 * acá para evitar drift cuando aparecen nuevos endpoints supervisor-only.
 *
 * <p>Defensa por roles: cualquier llamada con un user no-supervisor se
 * rechaza ANTES de tocar el repositorio. Defensa en profundidad respecto
 * a {@code @PreAuthorize} en el controller.
 */
public class SupervisorScopeResolver {

    private final SupervisorRepository supervisorRepository;

    public SupervisorScopeResolver(SupervisorRepository supervisorRepository) {
        this.supervisorRepository = Objects.requireNonNull(supervisorRepository, "supervisorRepository");
    }

    /**
     * Devuelve el set de IDs de practicantes que el supervisor tiene
     * vinculados. Si el supervisor no tiene practicantes a cargo se
     * devuelve set vacío (caso "docente recién dado de alta", panel vacío).
     */
    public Set<Long> resolveAllowedPractitionerIds(User supervisorUser) {
        ensureSupervisor(supervisorUser);
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supervisor", "userId", supervisorUser.getId().toString()));
        if (supervisor.getSupervisedPractitioners() == null) {
            return Collections.emptySet();
        }
        return supervisor.getSupervisedPractitioners().stream()
                .map(Practitioner::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Si el filtro {@code practitionerId} viene seteado y NO pertenece al
     * cerco, rechaza explícitamente con 403. No silenciamos para evitar que
     * el docente "explore" practicantes ajenos probando IDs.
     */
    public void rejectIfOutOfScope(Long practitionerId, Set<Long> allowedPractitionerIds) {
        if (practitionerId == null) {
            return;
        }
        if (allowedPractitionerIds == null || !allowedPractitionerIds.contains(practitionerId)) {
            throw new UnauthorizedOperationException(
                    "No puede consultar feedback de un practicante que no supervisa.");
        }
    }

    private void ensureSupervisor(User user) {
        if (user == null || user.getRole() != Role.ROLE_SUPERVISOR) {
            throw new UnauthorizedOperationException(
                    "Solo los supervisores pueden acceder a esta operación.");
        }
    }
}
