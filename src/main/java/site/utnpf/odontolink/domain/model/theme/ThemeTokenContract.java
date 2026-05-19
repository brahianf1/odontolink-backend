package site.utnpf.odontolink.domain.model.theme;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contrato de los 35 tokens de color que componen un {@code CustomTheme}.
 *
 * <p>El frontend renderiza dos paletas (light y dark) usando exactamente
 * estas 35 keys. Si alguna falta, la UI rompe; si el formato no es hex
 * {@code #rrggbb}, los componentes M3 fallan silenciosamente. Por eso
 * validamos en el backend antes de aceptar el theme: la fuente de verdad
 * del contrato vive aca para no depender de la disciplina del cliente.
 *
 * <p>El listado se mantiene como {@link Set} (no {@code List}) para tener
 * {@code O(1)} en el lookup y porque el orden no importa para validacion
 * (la salida JSON usa el orden del map que envia el cliente / Hibernate).
 *
 * <p>El metodo {@link #validate(Map, String)} concentra:
 * <ul>
 *   <li>Reporte agregado: junta TODOS los errores de la paleta y los
 *       devuelve en {@code details[]}. El admin no quiere descubrir los 35
 *       fallos de a uno; recibe todos en la primera respuesta.</li>
 *   <li>{@code errorCode} estable {@code INVALID_THEME_TOKENS} para que el
 *       frontend ramifique UX sin parsear el mensaje humano.</li>
 * </ul>
 */
public final class ThemeTokenContract {

    /**
     * Regex de un color hex de 6 chars con {@code #} delante. Es lo que el
     * estandar shadcn-style espera y lo que tailwind/M3 consumen sin
     * normalizacion adicional. Aceptar 3 chars ({@code #abc}) o variantes
     * con alpha rompe la simetria del catalogo.
     */
    public static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public static final String ERROR_CODE = "INVALID_THEME_TOKENS";

    /**
     * Las 35 keys obligatorias acordadas con el FE. Cualquier modificacion
     * de este set rompe el contrato y debe coordinarse con el agente del
     * frontend (la lista equivalente vive en su codigo).
     */
    public static final Set<String> REQUIRED_KEYS = Set.of(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer", "inversePrimary",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground", "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
            "surfaceDim", "surfaceBright",
            "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
            "surfaceContainerHigh", "surfaceContainerHighest",
            "outline", "outlineVariant", "shadow", "scrim", "inverseSurface", "inverseOnSurface", "surfaceTint",
            "chart1", "chart2", "chart3", "chart4", "chart5"
    );

    private ThemeTokenContract() {
        // utility
    }

    /**
     * Verifica que {@code tokens} contenga las 35 keys obligatorias y que
     * cada value matchee el formato hex. Junta todos los errores antes de
     * lanzar (no se corta en el primer fallo).
     *
     * @param tokens    paleta enviada por el cliente (light o dark).
     * @param fieldPath prefijo para identificar el campo en {@code details}
     *                  (e.g. {@code "light"} → {@code "light.primary: must match #rrggbb"}).
     */
    public static void validate(Map<String, String> tokens, String fieldPath) {
        List<String> details = new ArrayList<>();
        if (tokens == null) {
            details.add(fieldPath + ": missing object");
            throw new InvalidBusinessRuleException(
                    "Tokens invalidos en '" + fieldPath + "'.",
                    ERROR_CODE,
                    details);
        }
        for (String key : REQUIRED_KEYS) {
            String value = tokens.get(key);
            if (value == null) {
                details.add(fieldPath + "." + key + ": missing");
                continue;
            }
            if (!HEX_COLOR.matcher(value).matches()) {
                details.add(fieldPath + "." + key + ": must match #rrggbb");
            }
        }
        // No reportamos keys "extra" como error fatal: el frontend puede
        // querer enviar campos futuros que aun no validamos. Si en algun
        // momento queremos endurecer esto, se agrega un loop adicional.
        if (!details.isEmpty()) {
            throw new InvalidBusinessRuleException(
                    "Tokens invalidos en '" + fieldPath + "'.",
                    ERROR_CODE,
                    details);
        }
    }
}
