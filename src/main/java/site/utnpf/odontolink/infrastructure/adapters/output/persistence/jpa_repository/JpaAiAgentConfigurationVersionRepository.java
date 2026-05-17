package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationVersionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAiAgentConfigurationVersionRepository
        extends JpaRepository<AiAgentConfigurationVersionEntity, Long> {

    List<AiAgentConfigurationVersionEntity> findAllByOrderByVersionNumberDesc();

    Optional<AiAgentConfigurationVersionEntity> findByVersionNumber(int versionNumber);

    /**
     * Devuelve el numero de version mas alto persistido. La aplicacion lo
     * usa para asignar el siguiente al publicar; usar la BD como fuente
     * de verdad evita races entre nodos (aunque el deploy single-instance
     * actual no necesita la garantia, mantenerla aqui es barato).
     */
    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM AiAgentConfigurationVersionEntity v")
    int findMaxVersionNumber();
}
