package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.OfferedTreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaOfferedTreatmentRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.OfferedTreatmentPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PractitionerPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.TreatmentPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para OfferedTreatment (Hexagonal Architecture).
 * Implementa la interfaz del dominio OfferedTreatmentRepository usando JPA.
 * Puerto de salida (Output Adapter).
 */
@Component
public class OfferedTreatmentPersistenceAdapter implements OfferedTreatmentRepository {

    private final JpaOfferedTreatmentRepository jpaOfferedTreatmentRepository;

    public OfferedTreatmentPersistenceAdapter(JpaOfferedTreatmentRepository jpaOfferedTreatmentRepository) {
        this.jpaOfferedTreatmentRepository = jpaOfferedTreatmentRepository;
    }

    @Override
    public OfferedTreatment save(OfferedTreatment offeredTreatment) {
        OfferedTreatmentEntity entity = OfferedTreatmentPersistenceMapper.toEntity(offeredTreatment);
        OfferedTreatmentEntity savedEntity = jpaOfferedTreatmentRepository.save(entity);
        return OfferedTreatmentPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<OfferedTreatment> findById(Long id) {
        return jpaOfferedTreatmentRepository.findById(id)
                .map(OfferedTreatmentPersistenceMapper::toDomain);
    }

    @Override
    public List<OfferedTreatment> findByPractitioner(Practitioner practitioner) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        return jpaOfferedTreatmentRepository.findByPractitioner(practitionerEntity).stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfferedTreatment> findByPractitionerId(Long practitionerId) {
        return jpaOfferedTreatmentRepository.findByPractitionerId(practitionerId).stream()
                .map(OfferedTreatmentPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPractitionerAndTreatment(Practitioner practitioner, Treatment treatment) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        TreatmentEntity treatmentEntity = TreatmentPersistenceMapper.toEntity(treatment);
        return jpaOfferedTreatmentRepository.existsByPractitionerAndTreatment(practitionerEntity, treatmentEntity);
    }

    @Override
    public Optional<OfferedTreatment> findByPractitionerAndTreatment(Practitioner practitioner, Treatment treatment) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        TreatmentEntity treatmentEntity = TreatmentPersistenceMapper.toEntity(treatment);
        return jpaOfferedTreatmentRepository.findByPractitionerAndTreatment(practitionerEntity, treatmentEntity)
                .map(OfferedTreatmentPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaOfferedTreatmentRepository.deleteById(id);
    }

    @Override
    public boolean hasActiveAppointments(Long offeredTreatmentId) {
        return jpaOfferedTreatmentRepository.hasActiveAppointments(offeredTreatmentId);
    }
}
