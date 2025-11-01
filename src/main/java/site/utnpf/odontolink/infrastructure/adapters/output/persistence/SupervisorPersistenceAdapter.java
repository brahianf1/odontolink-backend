package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SupervisorEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaSupervisorRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.SupervisorPersistenceMapper;

import java.util.Optional;

/**
 * Adaptador de persistencia para Supervisor (Hexagonal Architecture).
 * Implementa la interfaz del dominio SupervisorRepository usando JPA.
 * Puerto de salida (Output Adapter).
 */
@Component
public class SupervisorPersistenceAdapter implements SupervisorRepository {

    private final JpaSupervisorRepository jpaSupervisorRepository;

    public SupervisorPersistenceAdapter(JpaSupervisorRepository jpaSupervisorRepository) {
        this.jpaSupervisorRepository = jpaSupervisorRepository;
    }

    @Override
    public Supervisor save(Supervisor supervisor) {
        SupervisorEntity entity = SupervisorPersistenceMapper.toEntity(supervisor);
        SupervisorEntity savedEntity = jpaSupervisorRepository.save(entity);
        return SupervisorPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Supervisor> findById(Long id) {
        return jpaSupervisorRepository.findById(id)
                .map(SupervisorPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Supervisor> findByUserId(Long userId) {
        return jpaSupervisorRepository.findByUserId(userId)
                .map(SupervisorPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmployeeId(String employeeId) {
        return jpaSupervisorRepository.existsByEmployeeId(employeeId);
    }
}
