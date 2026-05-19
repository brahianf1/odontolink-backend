package site.utnpf.odontolink.application.service.support;

import java.text.Normalizer;
import java.util.function.Predicate;

/**
 * Helper para generar slugs unicos en formato kebab-case a partir del
 * {@code name} legible que envia el admin.
 *
 * <p>Convencion:
 * <ul>
 *   <li>Prefijo {@code custom-}: marca al slug como "custom theme" frente a
 *       built-ins (e.g. {@code theme-14}). El endpoint publico usa este
 *       prefijo para decidir si embeber el theme en la respuesta.</li>
 *   <li>Resto kebab-case ASCII (sin acentos): minusculas + alfanumerico +
 *       guiones, sin acentos. Mejor compatibilidad con URLs si en algun
 *       momento se usa el slug en path.</li>
 *   <li>Colisiones: si el slug base ya existe, se appendea {@code -2},
 *       {@code -3}, etc., hasta encontrar uno libre.</li>
 * </ul>
 *
 * <p>El predicate {@code isTaken} se inyecta para mantener este helper
 * libre de dependencias de Spring/JPA: testeable y reutilizable.
 */
public final class SlugGenerator {

    private static final String CUSTOM_PREFIX = "custom-";
    /**
     * Tope defensivo de intentos para evitar bucles infinitos si el predicate
     * miente y siempre dice "taken". 999 sufijos son mas que suficientes en
     * un catalogo realista.
     */
    private static final int MAX_ATTEMPTS = 999;

    private SlugGenerator() {
    }

    /**
     * Genera un slug unico a partir del {@code name}. Garantiza que el
     * resultado NO sera reportado como taken por el predicate (modulo
     * race-conditions de BD, que el adapter resuelve con unique constraint).
     *
     * @throws IllegalStateException si tras {@link #MAX_ATTEMPTS} intentos
     *                               sigue sin encontrar un slug libre.
     */
    public static String generate(String name, Predicate<String> isTaken) {
        String base = normalize(name);
        if (base.isEmpty()) {
            // El admin envio un name con caracteres todos no-ASCII. Es raro
            // pero defendemos el caso para no caer en NPE corriente abajo.
            base = "theme";
        }
        String candidate = CUSTOM_PREFIX + base;
        if (!isTaken.test(candidate)) {
            return candidate;
        }
        for (int suffix = 2; suffix <= MAX_ATTEMPTS; suffix++) {
            String next = candidate + "-" + suffix;
            if (!isTaken.test(next)) {
                return next;
            }
        }
        throw new IllegalStateException(
                "No fue posible generar un slug unico para '" + name + "' tras "
                        + MAX_ATTEMPTS + " intentos.");
    }

    /**
     * Convierte un string libre en su forma kebab-case ASCII. Pasos:
     * normalizacion Unicode NFD + strip de marcas diacriticas + lowercase
     * + reemplazo de no-alfanumerico por guion + colapso de guiones
     * repetidos + trim de guiones extremos.
     */
    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String stripped = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lower = stripped.toLowerCase();
        String dashed = lower.replaceAll("[^a-z0-9]+", "-");
        String collapsed = dashed.replaceAll("-{2,}", "-");
        // Trim de guiones al inicio/fin: si despues queda vacio, devolvemos
        // string vacio y dejamos que el caller decida el fallback.
        if (collapsed.startsWith("-")) {
            collapsed = collapsed.substring(1);
        }
        if (collapsed.endsWith("-")) {
            collapsed = collapsed.substring(0, collapsed.length() - 1);
        }
        return collapsed;
    }
}
