package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaPatientRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PatientPersistenceMapper;

import java.util.Optional;

/**
 * Adaptador de persistencia para Patient (Hexagonal Architecture).
 * Implementa la interfaz del dominio PatientRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Ver nota sobre la politica transaccional en {@link UserPersistenceAdapter}:
 * el mapper toca {@code PatientEntity#user} (LAZY) y necesita una sesion
 * activa durante el mapeo.
 */
@Component
@Transactional(readOnly = true)
public class PatientPersistenceAdapter implements PatientRepository {

    private final JpaPatientRepository jpaPatientRepository;

    public PatientPersistenceAdapter(JpaPatientRepository jpaPatientRepository) {
        this.jpaPatientRepository = jpaPatientRepository;
    }

    @Override
    @Transactional
    public Patient save(Patient patient) {
        PatientEntity entity = PatientPersistenceMapper.toEntity(patient);
        PatientEntity savedEntity = jpaPatientRepository.save(entity);
        return PatientPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Patient> findById(Long id) {
        return jpaPatientRepository.findById(id)
                .map(PatientPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Patient> findByUserId(Long userId) {
        return jpaPatientRepository.findByUserId(userId)
                .map(PatientPersistenceMapper::toDomain);
    }
}
