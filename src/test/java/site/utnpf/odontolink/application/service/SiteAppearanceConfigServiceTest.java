package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase;
import site.utnpf.odontolink.application.service.support.SingletonRowBootstrap;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.domain.model.SiteAppearanceConfig;
import site.utnpf.odontolink.domain.model.SiteDefaultMode;
import site.utnpf.odontolink.domain.repository.CustomThemeRepository;
import site.utnpf.odontolink.domain.repository.SiteAppearanceConfigRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del {@link SiteAppearanceConfigService}. Verifican:
 * <ul>
 *   <li>Bootstrap: si el singleton no existe, se siembra con defaults.</li>
 *   <li>Update OK: incrementa version y persiste actorId.</li>
 *   <li>Update con expectedVersion stale: lanza
 *       {@link VersionConflictException} antes de tocar la BD.</li>
 *   <li>Update con themeVariantId custom inexistente: lanza
 *       {@link InvalidBusinessRuleException}.</li>
 *   <li>Embedded custom theme cuando el slug del singleton tiene prefijo
 *       {@code custom-} y el theme existe en BD.</li>
 * </ul>
 */
class SiteAppearanceConfigServiceTest {

    private SiteAppearanceConfigRepository repo;
    private CustomThemeRepository customThemeRepo;
    private SingletonRowBootstrap bootstrap;
    private SiteAppearanceConfigService service;

    @BeforeEach
    void setUp() {
        repo = mock(SiteAppearanceConfigRepository.class);
        customThemeRepo = mock(CustomThemeRepository.class);
        bootstrap = mock(SingletonRowBootstrap.class);
        service = new SiteAppearanceConfigService(repo, customThemeRepo, bootstrap);
    }

    @Test
    void getDevuelveDefaultsCuandoSingletonNoExiste() {
        // Simulamos que el bootstrap detecta empty y ejecuta el defaultFactory.
        SiteAppearanceConfig defaults = SiteAppearanceConfig.defaults();
        whenBootstrapReturns(defaults);

        var snapshot = service.get();

        assertEquals(SiteAppearanceConfig.DEFAULT_THEME_VARIANT_ID,
                snapshot.appearance().getThemeVariantId());
        assertEquals(1, snapshot.appearance().getVersion());
        // No es custom: no debe haberse consultado el repo de custom themes.
        verify(customThemeRepo, never()).findActiveBySlug(any());
    }

    @Test
    void getEmbebeElCustomThemeActivoCuandoSlugTienePrefijoCustom() {
        SiteAppearanceConfig config = new SiteAppearanceConfig(
                SiteAppearanceConfig.SINGLETON_ID,
                "custom-clinica-2024",
                "inter-source-jetbrains",
                SiteDefaultMode.LIGHT,
                false,
                5,
                42L,
                Instant.now()
        );
        whenBootstrapReturns(config);
        var customTheme = ThemeTestFixtures.aCustomTheme("custom-clinica-2024");
        when(customThemeRepo.findActiveBySlug("custom-clinica-2024"))
                .thenReturn(Optional.of(customTheme));

        var snapshot = service.get();

        assertEquals("custom-clinica-2024", snapshot.appearance().getThemeVariantId());
        assertEquals(customTheme.getSlug(),
                snapshot.activeCustomTheme().orElseThrow().getSlug());
    }

    @Test
    void updateIncrementaVersionYPersisteActorId() {
        SiteAppearanceConfig current = SiteAppearanceConfig.defaults();
        whenBootstrapReturns(current);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var snapshot = service.update(
                new ISiteAppearanceConfigUseCase.UpdateAppearanceCommand(
                        "theme-26", "outfit-inter-jetbrains",
                        SiteDefaultMode.DARK, true),
                1, // version actual
                42L);

        assertEquals("theme-26", snapshot.appearance().getThemeVariantId());
        assertEquals(2, snapshot.appearance().getVersion());
        assertEquals(42L, snapshot.appearance().getUpdatedBy());

        ArgumentCaptor<SiteAppearanceConfig> captor =
                ArgumentCaptor.forClass(SiteAppearanceConfig.class);
        verify(repo).save(captor.capture());
        assertEquals(SiteDefaultMode.DARK, captor.getValue().getDefaultMode());
    }

    @Test
    void updateConVersionStaleLanzaVersionConflictAntesDeTocarLaBD() {
        SiteAppearanceConfig current = SiteAppearanceConfig.defaults();
        // version actual = 1; el cliente envia expectedVersion=0 (stale).
        whenBootstrapReturns(current);

        VersionConflictException ex = assertThrows(
                VersionConflictException.class,
                () -> service.update(
                        new ISiteAppearanceConfigUseCase.UpdateAppearanceCommand(
                                "theme-26", "outfit-inter-jetbrains",
                                SiteDefaultMode.LIGHT, false),
                        0, 42L)
        );
        assertEquals(1, ex.getCurrentVersion());
        verify(repo, never()).save(any());
    }

    @Test
    void updateConCustomThemeInexistenteFalla() {
        SiteAppearanceConfig current = SiteAppearanceConfig.defaults();
        whenBootstrapReturns(current);
        when(customThemeRepo.existsActiveBySlug("custom-fantasma")).thenReturn(false);

        assertThrows(InvalidBusinessRuleException.class,
                () -> service.update(
                        new ISiteAppearanceConfigUseCase.UpdateAppearanceCommand(
                                "custom-fantasma", "inter-source-jetbrains",
                                SiteDefaultMode.SYSTEM, false),
                        1, 42L));
        verify(repo, never()).save(any());
    }

    // -------------------------------------------------------------------
    // Helpers de mocking del SingletonRowBootstrap. Como el helper es una
    // clase concreta (no interfaz), mockearlo aca nos exime de levantar un
    // PlatformTransactionManager: solo nos importa que devuelva el agregado
    // que pasamos. La firma de getOrCreate tiene tipos parametrizados, asi
    // que sobrecargamos el matcher con any() y un anyway-call al saver.
    // -------------------------------------------------------------------
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void whenBootstrapReturns(SiteAppearanceConfig value) {
        when(bootstrap.getOrCreate(any(Supplier.class), any(Supplier.class),
                any(Function.class), any(String.class)))
                .thenReturn(value);
    }
}
