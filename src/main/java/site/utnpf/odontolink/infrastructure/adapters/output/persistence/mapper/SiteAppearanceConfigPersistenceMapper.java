package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.SiteAppearanceConfigEntity;

/**
 * Mapper estatico entre {@link SiteAppearanceConfig} (dominio) y
 * {@link SiteAppearanceConfigEntity} (persistencia).
 *
 * <p>Sin estado: el patron es identico al de
 * {@code InstitutionalSettingsPersistenceMapper}.
 */
public final class SiteAppearanceConfigPersistenceMapper {

    private SiteAppearanceConfigPersistenceMapper() {
    }

    public static SiteAppearanceConfig toDomain(SiteAppearanceConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SiteAppearanceConfig(
                entity.getId(),
                entity.getThemeVariantId(),
                entity.getFontPairId(),
                entity.getDefaultMode(),
                entity.isAllowUserOverride(),
                entity.getVersion(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    public static SiteAppearanceConfigEntity toEntity(SiteAppearanceConfig domain) {
        if (domain == null) {
            return null;
        }
        SiteAppearanceConfigEntity entity = new SiteAppearanceConfigEntity();
        entity.setId(domain.getId());
        entity.setThemeVariantId(domain.getThemeVariantId());
        entity.setFontPairId(domain.getFontPairId());
        entity.setDefaultMode(domain.getDefaultMode());
        entity.setAllowUserOverride(domain.isAllowUserOverride());
        entity.setVersion(domain.getVersion());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
