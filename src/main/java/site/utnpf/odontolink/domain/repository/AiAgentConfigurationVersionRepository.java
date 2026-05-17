package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para los snapshots de publicaciones del agente IA
 * (RF31). Cada publish exitoso crea una nueva version.
 */
public interface AiAgentConfigurationVersionRepository {

    AiAgentConfigurationVersion save(AiAgentConfigurationVersion version);

    /**
     * Lista las versiones en orden descendente por numero. El admin las
     * ve mas recientes primero al revisar el historial.
     */
    List<AiAgentConfigurationVersion> findAllOrderByVersionNumberDesc();

    Optional<AiAgentConfigurationVersion> findByVersionNumber(int versionNumber);

    /**
     * Devuelve el numero de version mas alto persistido o 0 si no hay
     * publicaciones aun. El servicio lo usa para asignar el siguiente
     * numero al publicar.
     */
    int findMaxVersionNumber();
}
