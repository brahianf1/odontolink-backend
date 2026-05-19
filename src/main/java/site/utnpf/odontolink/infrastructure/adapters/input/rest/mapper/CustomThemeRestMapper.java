package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.ICustomThemeAdminUseCase;
import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CreateCustomThemeRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateCustomThemeRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.CustomThemeResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.CustomThemeSummaryResponseDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversiones REST <-> dominio para custom themes. Estilo: mapper estatico
 * sin estado, como el resto del codebase.
 */
public final class CustomThemeRestMapper {

    private CustomThemeRestMapper() {
    }

    public static ICustomThemeAdminUseCase.CreateCommand toCreateCommand(CreateCustomThemeRequestDTO dto) {
        return new ICustomThemeAdminUseCase.CreateCommand(
                dto.getName(),
                dto.getDescription(),
                dto.getMood(),
                dto.getFitScore(),
                dto.getTier(),
                dto.getDefaultFontPair(),
                copyOrNull(dto.getLight()),
                copyOrNull(dto.getDark()),
                dto.getSourceCss()
        );
    }

    public static ICustomThemeAdminUseCase.UpdateCommand toUpdateCommand(UpdateCustomThemeRequestDTO dto) {
        return new ICustomThemeAdminUseCase.UpdateCommand(
                dto.getName(),
                dto.getDescription(),
                dto.getMood(),
                dto.getFitScore(),
                dto.getTier(),
                dto.getDefaultFontPair(),
                copyOrNull(dto.getLight()),
                copyOrNull(dto.getDark()),
                dto.getSourceCss()
        );
    }

    public static CustomThemeResponseDTO toResponse(CustomTheme theme) {
        if (theme == null) {
            return null;
        }
        CustomThemeResponseDTO dto = new CustomThemeResponseDTO();
        dto.setId(theme.getId());
        dto.setSlug(theme.getSlug());
        dto.setName(theme.getName());
        dto.setDescription(theme.getDescription());
        dto.setMood(theme.getMood());
        dto.setFitScore(theme.getFitScore());
        dto.setTier(theme.getTier());
        dto.setDefaultFontPair(theme.getDefaultFontPair());
        dto.setLight(copyOrNull(theme.getLightTokens()));
        dto.setDark(copyOrNull(theme.getDarkTokens()));
        dto.setSourceCss(theme.getSourceCss());
        dto.setVersion(theme.getVersion());
        dto.setCreatedAt(theme.getCreatedAt());
        dto.setUpdatedAt(theme.getUpdatedAt());
        return dto;
    }

    public static CustomThemeSummaryResponseDTO toSummary(CustomTheme theme) {
        CustomThemeSummaryResponseDTO dto = new CustomThemeSummaryResponseDTO();
        dto.setId(theme.getId());
        dto.setSlug(theme.getSlug());
        dto.setName(theme.getName());
        dto.setDescription(theme.getDescription());
        dto.setMood(theme.getMood());
        dto.setFitScore(theme.getFitScore());
        dto.setTier(theme.getTier());
        dto.setDefaultFontPair(theme.getDefaultFontPair());
        dto.setLight(copyOrNull(theme.getLightTokens()));
        dto.setDark(copyOrNull(theme.getDarkTokens()));
        dto.setVersion(theme.getVersion());
        dto.setCreatedAt(theme.getCreatedAt());
        dto.setUpdatedAt(theme.getUpdatedAt());
        return dto;
    }

    public static List<CustomThemeSummaryResponseDTO> toSummaryList(List<CustomTheme> themes) {
        return themes.stream().map(CustomThemeRestMapper::toSummary).toList();
    }

    private static Map<String, String> copyOrNull(Map<String, String> source) {
        return source == null ? null : new LinkedHashMap<>(source);
    }
}
