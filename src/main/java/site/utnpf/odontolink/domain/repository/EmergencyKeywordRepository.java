package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.EmergencyKeyword;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para el diccionario de emergencias del chatbot (RF32).
 */
public interface EmergencyKeywordRepository {

    List<EmergencyKeyword> findAllOrderByTermAsc();

    List<EmergencyKeyword> findAllActive();

    Optional<EmergencyKeyword> findById(Long id);

    Optional<EmergencyKeyword> findByTermIgnoreCase(String term);

    EmergencyKeyword save(EmergencyKeyword keyword);

    void deleteById(Long id);
}
