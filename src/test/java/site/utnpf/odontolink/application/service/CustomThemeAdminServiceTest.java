package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.utnpf.odontolink.application.port.in.ICustomThemeAdminUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ThemeInUseException;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.model.CustomThemeTier;
import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;
import site.utnpf.odontolink.domain.model.theme.ThemeTokenContract;
import site.utnpf.odontolink.domain.repository.CustomThemeRepository;
import site.utnpf.odontolink.domain.repository.SiteAppearanceConfigRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomThemeAdminServiceTest {

    private CustomThemeRepository repo;
    private SiteAppearanceConfigRepository appearanceRepo;
    private CustomThemeAdminService service;

    @BeforeEach
    void setUp() {
        repo = mock(CustomThemeRepository.class);
        appearanceRepo = mock(SiteAppearanceConfigRepository.class);
        service = new CustomThemeAdminService(repo, appearanceRepo);
        when(repo.save(any())).thenAnswer(inv -> {
            CustomTheme t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(1L);
            }
            return t;
        });
    }

    @Test
    void createPersisteThemeConSlugAutogenerado() {
        when(repo.existsActiveBySlug("custom-clinica-2024")).thenReturn(false);

        CustomTheme created = service.create(buildCreateCmd("Clinica 2024", "#112233", "#445566"), 99L);

        assertEquals("custom-clinica-2024", created.getSlug());
        assertEquals(99L, created.getCreatedBy());
        assertEquals(1, created.getVersion());
        verify(repo).save(any(CustomTheme.class));
    }

    @Test
    void createConNombreDuplicadoGeneraSlugConSufijo() {
        // El primer slug colisiona; el segundo intento queda libre.
        when(repo.existsActiveBySlug("custom-clinica-2024")).thenReturn(true);
        when(repo.existsActiveBySlug("custom-clinica-2024-2")).thenReturn(false);

        CustomTheme created = service.create(buildCreateCmd("Clinica 2024", "#112233", "#445566"), 99L);

        assertEquals("custom-clinica-2024-2", created.getSlug());
    }

    @Test
    void createConTokenInvalidoLanzaInvalidBusinessRuleConDetails() {
        when(repo.existsActiveBySlug(any())).thenReturn(false);

        Map<String, String> badLight = ThemeTestFixtures.validTokens("#112233");
        // Rompemos un valor: el regex hex debe fallar y reportarlo en details.
        badLight.put("primary", "#zzz");

        InvalidBusinessRuleException ex = assertThrows(
                InvalidBusinessRuleException.class,
                () -> service.create(
                        new ICustomThemeAdminUseCase.CreateCommand(
                                "Clinica", "", "", 3, CustomThemeTier.OFFICIAL,
                                "inter-source-jetbrains",
                                badLight,
                                ThemeTestFixtures.validTokens("#445566"),
                                ":root {}"
                        ),
                        99L)
        );
        assertEquals(ThemeTokenContract.ERROR_CODE, ex.getErrorCode());
        assertTrue(ex.getDetails().stream()
                        .anyMatch(d -> d.startsWith("light.primary:")),
                "Debe reportar el path exacto del token invalido. Actual: " + ex.getDetails());
        verify(repo, never()).save(any());
    }

    @Test
    void createConTokenFaltanteLanzaInvalidBusinessRuleConDetails() {
        when(repo.existsActiveBySlug(any())).thenReturn(false);

        Map<String, String> incompleteDark = new LinkedHashMap<>(
                ThemeTestFixtures.validTokens("#445566"));
        incompleteDark.remove("surfaceTint"); // sacamos una key obligatoria

        InvalidBusinessRuleException ex = assertThrows(
                InvalidBusinessRuleException.class,
                () -> service.create(
                        new ICustomThemeAdminUseCase.CreateCommand(
                                "Clinica", "", "", 3, CustomThemeTier.OFFICIAL,
                                "inter-source-jetbrains",
                                ThemeTestFixtures.validTokens("#112233"),
                                incompleteDark,
                                ":root {}"
                        ),
                        99L)
        );
        assertTrue(ex.getDetails().stream()
                .anyMatch(d -> d.equals("dark.surfaceTint: missing")));
    }

    @Test
    void updateRespetaOptimisticLocking() {
        CustomTheme existing = ThemeTestFixtures.aCustomTheme("custom-test");
        existing.setId(7L);
        existing.setVersion(3);
        when(repo.findActiveById(7L)).thenReturn(Optional.of(existing));

        // Cliente envia version=2 (stale frente al 3 actual).
        VersionConflictException ex = assertThrows(
                VersionConflictException.class,
                () -> service.update(7L,
                        buildUpdateCmd("Renombrado", "#112233", "#445566"),
                        2,
                        99L));
        assertEquals(3, ex.getCurrentVersion());
        // El save NO debe haber ocurrido tras el conflict.
        verify(repo, times(0)).save(any());
    }

    @Test
    void updateOKIncrementaVersion() {
        CustomTheme existing = ThemeTestFixtures.aCustomTheme("custom-test");
        existing.setId(7L);
        existing.setVersion(3);
        when(repo.findActiveById(7L)).thenReturn(Optional.of(existing));

        CustomTheme updated = service.update(7L,
                buildUpdateCmd("Renombrado", "#112233", "#445566"),
                3,
                99L);
        assertEquals(4, updated.getVersion());
        assertEquals("Renombrado", updated.getName());
        assertEquals(99L, updated.getUpdatedBy());
    }

    @Test
    void softDeleteFallaConThemeInUseSiEsElActivoEnAppearance() {
        CustomTheme existing = ThemeTestFixtures.aCustomTheme("custom-active");
        existing.setId(7L);
        when(repo.findActiveById(7L)).thenReturn(Optional.of(existing));

        SiteAppearanceConfig singleton = new SiteAppearanceConfig(
                SiteAppearanceConfig.SINGLETON_ID,
                "custom-active",
                "inter-source-jetbrains",
                SiteDefaultMode.SYSTEM,
                false,
                1,
                null,
                Instant.now()
        );
        when(appearanceRepo.findSingleton()).thenReturn(Optional.of(singleton));

        ThemeInUseException ex = assertThrows(ThemeInUseException.class,
                () -> service.softDelete(7L, 99L));
        assertEquals("custom-active", ex.getSlug());
        verify(repo, never()).save(any());
    }

    @Test
    void softDeleteOKMarcaDeletedAtYActorId() {
        CustomTheme existing = ThemeTestFixtures.aCustomTheme("custom-orphan");
        existing.setId(7L);
        when(repo.findActiveById(7L)).thenReturn(Optional.of(existing));
        SiteAppearanceConfig singleton = new SiteAppearanceConfig(
                SiteAppearanceConfig.SINGLETON_ID,
                "theme-14", // built-in, no apunta al theme que estamos borrando
                "inter-source-jetbrains",
                SiteDefaultMode.SYSTEM,
                false,
                1,
                null,
                Instant.now()
        );
        when(appearanceRepo.findSingleton()).thenReturn(Optional.of(singleton));

        service.softDelete(7L, 99L);

        ArgumentCaptor<CustomTheme> captor = ArgumentCaptor.forClass(CustomTheme.class);
        verify(repo).save(captor.capture());
        CustomTheme saved = captor.getValue();
        assertNotNull(saved.getDeletedAt(), "Soft delete debe setear deletedAt");
        assertTrue(saved.isDeleted());
        assertEquals(99L, saved.getUpdatedBy());
    }

    // -- helpers --------------------------------------------------------

    private ICustomThemeAdminUseCase.CreateCommand buildCreateCmd(String name,
                                                                  String lightHex,
                                                                  String darkHex) {
        return new ICustomThemeAdminUseCase.CreateCommand(
                name, "", "", 3, CustomThemeTier.OFFICIAL,
                "inter-source-jetbrains",
                ThemeTestFixtures.validTokens(lightHex),
                ThemeTestFixtures.validTokens(darkHex),
                ":root { --primary: " + lightHex + "; }"
        );
    }

    private ICustomThemeAdminUseCase.UpdateCommand buildUpdateCmd(String name,
                                                                  String lightHex,
                                                                  String darkHex) {
        return new ICustomThemeAdminUseCase.UpdateCommand(
                name, "", "", 3, CustomThemeTier.OFFICIAL,
                "inter-source-jetbrains",
                ThemeTestFixtures.validTokens(lightHex),
                ThemeTestFixtures.validTokens(darkHex),
                ":root { --primary: " + lightHex + "; }"
        );
    }
}
