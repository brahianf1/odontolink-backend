package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AiAgentConfiguration;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de la configuracion del agente IA
 * (RF31, RF32). Sigue el patron singleton de
 * {@link InstitutionalSettingsRepository}: solo se exponen las operaciones
 * que el caso de uso necesita, sin ampliar superficie con metodos que no
 * tienen sentido en un agregado unico.
 */
public interface AiAgentConfigurationRepository {

    Optional<AiAgentConfiguration> findSingleton();

    AiAgentConfiguration save(AiAgentConfiguration configuration);
}
