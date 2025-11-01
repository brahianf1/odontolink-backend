package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Supervisor;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de supervisores (Hexagonal Architecture).
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura.
 */
public interface SupervisorRepository {
    Supervisor save(Supervisor supervisor);
    Optional<Supervisor> findById(Long id);
    Optional<Supervisor> findByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
}
