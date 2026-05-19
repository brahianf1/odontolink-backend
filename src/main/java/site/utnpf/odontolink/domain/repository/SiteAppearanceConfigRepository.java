package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de la configuracion de appearance
 * singleton (RF-site-appearance). Sigue el patron de
 * {@link AiAgentConfigurationRepository} e
 * {@link InstitutionalSettingsRepository}: API minima, solo lo que el caso
 * de uso necesita.
 */
public interface SiteAppearanceConfigRepository {

    Optional<SiteAppearanceConfig> findSingleton();

    SiteAppearanceConfig save(SiteAppearanceConfig config);
}
