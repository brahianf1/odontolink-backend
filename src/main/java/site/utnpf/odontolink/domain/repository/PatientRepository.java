package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Patient;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de pacientes (Hexagonal Architecture).
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura.
 */
public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(Long id);
    Optional<Patient> findByUserId(Long userId);
}
