package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.EmergencyKeywordEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaEmergencyKeywordRepository extends JpaRepository<EmergencyKeywordEntity, Long> {

    List<EmergencyKeywordEntity> findAllByOrderByTermAsc();

    List<EmergencyKeywordEntity> findAllByActiveTrueOrderByTermAsc();

    Optional<EmergencyKeywordEntity> findFirstByTermIgnoreCase(String term);
}
