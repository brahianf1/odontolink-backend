package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaPractitionerRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PractitionerPersistenceMapper;

import java.util.Optional;

/**
 * Adaptador de persistencia para Practitioner (Hexagonal Architecture).
 * Implementa la interfaz del dominio PractitionerRepository usando JPA.
 * Puerto de salida (Output Adapter).
 */
@Component
public class PractitionerPersistenceAdapter implements PractitionerRepository {

    private final JpaPractitionerRepository jpaPractitionerRepository;

    public PractitionerPersistenceAdapter(JpaPractitionerRepository jpaPractitionerRepository) {
        this.jpaPractitionerRepository = jpaPractitionerRepository;
    }

    @Override
    public Practitioner save(Practitioner practitioner) {
        PractitionerEntity entity = PractitionerPersistenceMapper.toEntity(practitioner);
        PractitionerEntity savedEntity = jpaPractitionerRepository.save(entity);
        return PractitionerPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Practitioner> findById(Long id) {
        return jpaPractitionerRepository.findById(id)
                .map(PractitionerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Practitioner> findByUserId(Long userId) {
        return jpaPractitionerRepository.findByUserId(userId)
                .map(PractitionerPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByStudentId(String studentId) {
        return jpaPractitionerRepository.existsByStudentId(studentId);
    }
}
