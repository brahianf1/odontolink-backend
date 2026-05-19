package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.CustomThemeEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapper estatico entre {@link CustomTheme} (dominio) y
 * {@link CustomThemeEntity} (persistencia).
 *
 * <p>Los mapas {@code lightTokens} y {@code darkTokens} se copian a
 * {@link LinkedHashMap} para preservar el orden de inserción si el cliente
 * dependiera de él, y para desacoplar la instancia del dominio del estado
 * interno de Hibernate (que puede mutar el map al persistir).
 */
public final class CustomThemePersistenceMapper {

    private CustomThemePersistenceMapper() {
    }

    public static CustomTheme toDomain(CustomThemeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CustomTheme(
                entity.getId(),
                entity.getSlug(),
                entity.getName(),
                entity.getDescription(),
                entity.getMood(),
                entity.getFitScore(),
                entity.getTier(),
                entity.getDefaultFontPair(),
                copyOrNull(entity.getLightTokens()),
                copyOrNull(entity.getDarkTokens()),
                entity.getSourceCss(),
                entity.getVersion(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    public static CustomThemeEntity toEntity(CustomTheme domain) {
        if (domain == null) {
            return null;
        }
        CustomThemeEntity entity = new CustomThemeEntity();
        entity.setId(domain.getId());
        entity.setSlug(domain.getSlug());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setMood(domain.getMood());
        entity.setFitScore(domain.getFitScore());
        entity.setTier(domain.getTier());
        entity.setDefaultFontPair(domain.getDefaultFontPair());
        entity.setLightTokens(copyOrNull(domain.getLightTokens()));
        entity.setDarkTokens(copyOrNull(domain.getDarkTokens()));
        entity.setSourceCss(domain.getSourceCss());
        entity.setVersion(domain.getVersion());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        return entity;
    }

    private static Map<String, String> copyOrNull(Map<String, String> source) {
        return source == null ? null : new LinkedHashMap<>(source);
    }
}
