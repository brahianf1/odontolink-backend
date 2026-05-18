package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AiAgentAccessMode;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiPiiPolicy;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationEntity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper estatico entre {@link AiAgentConfiguration} (dominio) y
 * {@link AiAgentConfigurationEntity} (persistencia).
 *
 * <p>Maneja la migracion suave de los campos chatbot (RF29/RF31/RF32/RF34):
 * si la fila singleton existente fue creada en una version previa, las
 * columnas nuevas vienen null. En ese caso aplicamos defaults conservadores
 * (DISABLED + BLOCK + buffer 20) para que la app levante sin romperse y el
 * admin pueda completar la config con un PUT.
 */
public final class AiAgentConfigurationPersistenceMapper {

    private AiAgentConfigurationPersistenceMapper() {
    }

    public static AiAgentConfiguration toDomain(AiAgentConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AiAgentConfiguration(
                entity.getId(),
                entity.getDisplayName(),
                entity.getSystemPromptCore(),
                entity.getWelcomeMessage(),
                entity.getTemperature(),
                entity.getTopP(),
                entity.getMaxTokens(),
                entity.getK(),
                AiRetrievalMethod.valueOf(entity.getRetrievalMethod()),
                AiAgentLifecycle.valueOf(entity.getLifecycle()),
                entity.getProviderAgentId(),
                entity.getProviderSyncedAt(),
                entity.getLastSyncError(),
                entity.getUpdatedAt(),
                parseAccessMode(entity.getAccessMode()),
                parseAllowedRoles(entity.getAllowedRolesCsv()),
                parsePiiPolicy(entity.getPiiPolicy()),
                entity.getConversationBufferSize() == null ? 20 : entity.getConversationBufferSize(),
                entity.getRateLimitAnonymousPerHour() == null ? 20 : entity.getRateLimitAnonymousPerHour(),
                entity.getRateLimitAuthenticatedPerHour() == null ? 60 : entity.getRateLimitAuthenticatedPerHour(),
                entity.getAgentInvocationUrl(),
                entity.getEmergencyBannerText() == null
                        ? AiAgentConfiguration.DEFAULT_EMERGENCY_BANNER : entity.getEmergencyBannerText()
        );
    }

    public static AiAgentConfigurationEntity toEntity(AiAgentConfiguration domain) {
        if (domain == null) {
            return null;
        }
        AiAgentConfigurationEntity entity = new AiAgentConfigurationEntity();
        entity.setId(domain.getId());
        entity.setDisplayName(domain.getDisplayName());
        entity.setSystemPromptCore(domain.getSystemPromptCore());
        entity.setWelcomeMessage(domain.getWelcomeMessage());
        entity.setTemperature(domain.getTemperature());
        entity.setTopP(domain.getTopP());
        entity.setMaxTokens(domain.getMaxTokens());
        entity.setK(domain.getK());
        entity.setRetrievalMethod(domain.getRetrievalMethod().name());
        entity.setLifecycle(domain.getLifecycle().name());
        entity.setProviderAgentId(domain.getProviderAgentId());
        entity.setProviderSyncedAt(domain.getProviderSyncedAt());
        entity.setLastSyncError(domain.getLastSyncError());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAccessMode(domain.getAccessMode() == null ? null : domain.getAccessMode().name());
        entity.setAllowedRolesCsv(formatAllowedRoles(domain.getAllowedRoles()));
        entity.setPiiPolicy(domain.getPiiPolicy() == null ? null : domain.getPiiPolicy().name());
        entity.setConversationBufferSize(domain.getConversationBufferSize());
        entity.setRateLimitAnonymousPerHour(domain.getRateLimitAnonymousPerHour());
        entity.setRateLimitAuthenticatedPerHour(domain.getRateLimitAuthenticatedPerHour());
        entity.setAgentInvocationUrl(domain.getAgentInvocationUrl());
        entity.setEmergencyBannerText(domain.getEmergencyBannerText());
        return entity;
    }

    private static AiAgentAccessMode parseAccessMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return AiAgentAccessMode.DISABLED;
        }
        try {
            return AiAgentAccessMode.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return AiAgentAccessMode.DISABLED;
        }
    }

    private static AiPiiPolicy parsePiiPolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            return AiPiiPolicy.BLOCK;
        }
        try {
            return AiPiiPolicy.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return AiPiiPolicy.BLOCK;
        }
    }

    private static Set<Role> parseAllowedRoles(String csv) {
        if (csv == null || csv.isBlank()) {
            return EnumSet.noneOf(Role.class);
        }
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        for (String token : csv.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                roles.add(Role.valueOf(trimmed));
            } catch (IllegalArgumentException ex) {
                // Ignoramos tokens invalidos defensivamente para no romper
                // el bootstrap si alguien edita la BD a mano.
            }
        }
        return roles;
    }

    private static String formatAllowedRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    // Helper aux: evita warning de unused import en static analyzers
    @SuppressWarnings("unused")
    private static String[] empty() {
        return Arrays.copyOf(new String[0], 0);
    }
}
