package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.TreatmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaTreatmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.TreatmentPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Treatment (Hexagonal Architecture).
 * Implementa la interfaz del dominio TreatmentRepository usando JPA.
 * Puerto de salida (Output Adapter).
 */
@Component
public class TreatmentPersistenceAdapter implements TreatmentRepository {

    private final JpaTreatmentRepository jpaTreatmentRepository;

    public TreatmentPersistenceAdapter(JpaTreatmentRepository jpaTreatmentRepository) {
        this.jpaTreatmentRepository = jpaTreatmentRepository;
    }

    @Override
    public Treatment save(Treatment treatment) {
        TreatmentEntity entity = TreatmentPersistenceMapper.toEntity(treatment);
        TreatmentEntity savedEntity = jpaTreatmentRepository.save(entity);
        return TreatmentPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Treatment> findById(Long id) {
        return jpaTreatmentRepository.findById(id)
                .map(TreatmentPersistenceMapper::toDomain);
    }

    @Override
    public List<Treatment> findAll() {
        return jpaTreatmentRepository.findAll().stream()
                .map(TreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return jpaTreatmentRepository.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        jpaTreatmentRepository.deleteById(id);
    }
}
