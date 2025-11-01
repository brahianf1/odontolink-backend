package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Practitioner;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de practicantes (Hexagonal Architecture).
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura.
 */
public interface PractitionerRepository {
    Practitioner save(Practitioner practitioner);
    Optional<Practitioner> findById(Long id);
    Optional<Practitioner> findByUserId(Long userId);
    boolean existsByStudentId(String studentId);
}
