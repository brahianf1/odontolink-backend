package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.AttentionStatus;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AttentionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAttentionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AttentionPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PractitionerPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.TreatmentPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Attention (Hexagonal Architecture).
 * Implementa la interfaz del dominio AttentionRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Este adaptador maneja la persistencia transaccional del agregado Attention
 * junto con sus Appointments hijos gracias a CascadeType.ALL.
 */
@Component
public class AttentionPersistenceAdapter implements AttentionRepository {

    private final JpaAttentionRepository jpaAttentionRepository;

    public AttentionPersistenceAdapter(JpaAttentionRepository jpaAttentionRepository) {
        this.jpaAttentionRepository = jpaAttentionRepository;
    }

    /**
     * Guarda una Attention y sus Appointments asociados en una sola transacción.
     * Gracias a CascadeType.ALL en AttentionEntity, los Appointments se guardan automáticamente.
     */
    @Override
    public Attention save(Attention attention) {
        AttentionEntity entity = AttentionPersistenceMapper.toEntity(attention);
        AttentionEntity savedEntity = jpaAttentionRepository.save(entity);
        return AttentionPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Attention> findById(Long id) {
        return jpaAttentionRepository.findById(id)
                .map(AttentionPersistenceMapper::toDomain);
    }

    @Override
    public List<Attention> findByPatient(Patient patient) {
        return findByPatientId(patient.getId());
    }

    @Override
    public List<Attention> findByPatientId(Long patientId) {
        return jpaAttentionRepository.findByPatient_Id(patientId).stream()
                .map(AttentionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Attention> findByPractitioner(Practitioner practitioner) {
        return findByPractitionerId(practitioner.getId());
    }

    @Override
    public List<Attention> findByPractitionerId(Long practitionerId) {
        return jpaAttentionRepository.findByPractitioner_Id(practitionerId).stream()
                .map(AttentionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Attention> findByPatientIdAndStatus(Long patientId, AttentionStatus status) {
        return jpaAttentionRepository.findByPatient_IdAndStatus(patientId, status).stream()
                .map(AttentionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Attention> findByPractitionerIdAndStatus(Long practitionerId, AttentionStatus status) {
        return jpaAttentionRepository.findByPractitioner_IdAndStatus(practitionerId, status).stream()
                .map(AttentionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
            Long patientId,
            Long practitionerId,
            Long treatmentId,
            AttentionStatus status) {
        return jpaAttentionRepository.existsByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
                patientId,
                practitionerId,
                treatmentId,
                status
        );
    }

    @Override
    public Optional<Attention> findByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
            Long patientId,
            Long practitionerId,
            Long treatmentId,
            AttentionStatus status) {
        return jpaAttentionRepository.findByPatient_IdAndPractitioner_IdAndTreatment_IdAndStatus(
                        patientId,
                        practitionerId,
                        treatmentId,
                        status
                )
                .map(AttentionPersistenceMapper::toDomain);
    }

    @Override
    public int countByPractitionerAndTreatmentAndStatus(
            Practitioner practitioner,
            Treatment treatment,
            AttentionStatus status) {
        return jpaAttentionRepository.countByPractitioner_IdAndTreatment_IdAndStatus(
                practitioner.getId(),
                treatment.getId(),
                status
        );
    }
}
