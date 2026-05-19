package site.utnpf.odontolink.application.service.support;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del {@link SlugGenerator}.
 *
 * <p>Cubre normalizacion (acentos, mayusculas, espacios, simbolos), prefijo
 * {@code custom-}, y resolucion de colisiones con sufijos numericos.
 */
class SlugGeneratorTest {

    @Test
    void normalizaAcentosYEspaciosEnKebabCase() {
        String slug = SlugGenerator.generate("Clinica 2024", any -> false);
        assertEquals("custom-clinica-2024", slug);
    }

    @Test
    void normalizaSimbolosYColapsaGuiones() {
        String slug = SlugGenerator.generate("¡Hola! - Tema Bonito!", any -> false);
        // "¡Hola! - Tema Bonito!" → "hola-tema-bonito" tras normalizar
        // (los simbolos colapsan a un solo guion).
        assertTrue(slug.startsWith("custom-"));
        assertTrue(slug.contains("hola"));
        assertTrue(slug.contains("tema-bonito"));
        // No debe haber guiones duplicados.
        assertFalse(slug.contains("--"));
    }

    @Test
    void appendeaSufijoCuandoElSlugBaseColisiona() {
        Set<String> taken = new HashSet<>();
        taken.add("custom-clinica");
        String slug = SlugGenerator.generate("Clinica", taken::contains);
        assertEquals("custom-clinica-2", slug);
    }

    @Test
    void escalaSufijoHastaEncontrarUnoLibre() {
        Set<String> taken = new HashSet<>();
        taken.add("custom-clinica");
        taken.add("custom-clinica-2");
        taken.add("custom-clinica-3");
        String slug = SlugGenerator.generate("Clinica", taken::contains);
        assertEquals("custom-clinica-4", slug);
    }

    @Test
    void faltbackACustomThemeSiTodoElNombreEsNoAscii() {
        // Nombre con caracteres exclusivamente no ASCII: tras normalizar, el
        // resultado queda vacio y el helper deberia caer al fallback.
        String slug = SlugGenerator.generate("漢字漢字", any -> false);
        assertEquals("custom-theme", slug);
    }
}
