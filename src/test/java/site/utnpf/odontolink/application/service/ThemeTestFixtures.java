package site.utnpf.odontolink.application.service;

import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.domain.model.CustomThemeTier;
import site.utnpf.odontolink.domain.model.theme.ThemeTokenContract;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixtures compartidos por los tests del modulo de site appearance. Vive
 * en {@code application.service} porque ambos tests de servicio lo
 * comparten y no tiene sentido replicarlo.
 */
public final class ThemeTestFixtures {

    private ThemeTestFixtures() {
    }

    /**
     * Construye un map con las 35 keys obligatorias del contrato, todas
     * pintadas con el mismo color hex. Util para tests que no necesitan
     * paletas realistas, solo validas.
     */
    public static Map<String, String> validTokens(String hex) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : ThemeTokenContract.REQUIRED_KEYS) {
            map.put(key, hex);
        }
        return map;
    }

    public static CustomTheme aCustomTheme(String slug) {
        Instant now = Instant.now();
        return new CustomTheme(
                7L,
                slug,
                "Test theme",
                "desc",
                "neutral",
                3,
                CustomThemeTier.OFFICIAL,
                "inter-source-jetbrains",
                validTokens("#112233"),
                validTokens("#445566"),
                ":root { --primary: #112233; }",
                1,
                1L,
                now,
                null,
                now,
                null
        );
    }
}
