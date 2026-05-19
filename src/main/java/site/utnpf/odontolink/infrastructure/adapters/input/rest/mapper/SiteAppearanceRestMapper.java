package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase.AppearanceSnapshot;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateSiteAppearanceRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.CustomThemeResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.SiteAppearanceResponseDTO;

/**
 * Conversiones REST <-> dominio para la appearance singleton.
 */
public final class SiteAppearanceRestMapper {

    private SiteAppearanceRestMapper() {
    }

    public static ISiteAppearanceConfigUseCase.UpdateAppearanceCommand toCommand(
            UpdateSiteAppearanceRequestDTO dto) {
        return new ISiteAppearanceConfigUseCase.UpdateAppearanceCommand(
                dto.getThemeVariantId(),
                dto.getFontPairId(),
                dto.getDefaultMode(),
                Boolean.TRUE.equals(dto.getAllowUserOverride())
        );
    }

    public static SiteAppearanceResponseDTO toResponse(AppearanceSnapshot snapshot) {
        SiteAppearanceResponseDTO dto = new SiteAppearanceResponseDTO();
        dto.setThemeVariantId(snapshot.appearance().getThemeVariantId());
        dto.setFontPairId(snapshot.appearance().getFontPairId());
        dto.setDefaultMode(snapshot.appearance().getDefaultMode());
        dto.setAllowUserOverride(snapshot.appearance().isAllowUserOverride());
        dto.setVersion(snapshot.appearance().getVersion());
        // Embedded del custom theme cuando el slug singleton apunta a uno.
        // Aprovechamos el mismo mapper para tener exactamente el mismo shape
        // que devuelve el endpoint admin de detalle.
        snapshot.activeCustomTheme()
                .map(CustomThemeRestMapper::toResponse)
                .ifPresent(dto::setActiveCustomTheme);
        return dto;
    }

    /**
     * Construye el valor del header {@code ETag} a partir de la version
     * actual. Formato fuerte ({@code "v<n>"}) entre comillas dobles segun
     * RFC 7232. Spring agrega/quita las comillas automaticamente cuando se
     * usa {@code ResponseEntity.eTag(...)}, pero cuando comparamos el header
     * {@code If-None-Match} crudo necesitamos el valor completo con
     * comillas.
     */
    public static String etagFor(int version) {
        return "\"v" + version + "\"";
    }

    /**
     * Devuelve la version como string para el ETag de un {@link CustomThemeResponseDTO}
     * (no usado actualmente porque la appearance singleton es el unico
     * recurso con ETag publico, pero util si se expande a mas endpoints).
     */
    @SuppressWarnings("unused")
    public static String etagForCustomTheme(CustomThemeResponseDTO dto) {
        return "\"v" + dto.getVersion() + "\"";
    }
}
