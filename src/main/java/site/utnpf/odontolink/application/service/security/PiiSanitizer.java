package site.utnpf.odontolink.application.service.security;

import site.utnpf.odontolink.domain.model.ChatbotPiiType;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitizador local de PII para mensajes del chatbot (RF31, RF32).
 *
 * <p>Defensa en profundidad: aunque los guardrails del proveedor ya intenten
 * filtrar PII, no podemos confiar en eso al 100% porque los LLMs son no
 * deterministas. Aqui filtramos LOCALMENTE antes del envio.
 *
 * <p>Por que regex (en vez de Microsoft Presidio, Stanford NER, Spring AI):
 * <ul>
 *   <li>Footprint cero: no agrega dependencias ni latencia.</li>
 *   <li>Los patrones que nos importan (DNI/CUIT/CBU AR + tarjetas globales)
 *       son <em>estrictamente regulares</em>. Un NER ML los detectaria con
 *       similar precision pero a 50-200ms por mensaje.</li>
 *   <li>El idioma del paciente es es-AR; los modelos NER en espanol son
 *       pesados y aportan poco para este dominio.</li>
 *   <li>Tests unitarios cubren los falsos positivos comunes (numeros random
 *       sin estructura PII).</li>
 * </ul>
 *
 * <p>El sanitizador es deterministico y stateless. Una unica instancia
 * funciona para toda la app (los patrones son thread-safe via Pattern).
 */
public class PiiSanitizer {

    // Patrones compilados una sola vez, thread-safe por contrato de Pattern.

    /**
     * DNI argentino: 7 u 8 digitos, con o sin puntos de miles. Aceptamos
     * "1.234.567", "12.345.678", "12345678", "1234567". Boundaries con \b
     * para no matchear como parte de un numero mas largo.
     */
    private static final Pattern DNI = Pattern.compile(
            "\\b\\d{1,2}\\.?\\d{3}\\.?\\d{3}\\b"
    );

    /** CUIT/CUIL: 2 digitos - 8 digitos - 1 digito, con prefijos validos. */
    private static final Pattern CUIT = Pattern.compile(
            "\\b(20|23|24|27|30|33|34)-?\\d{8}-?\\d\\b"
    );

    /** CBU argentino: exactamente 22 digitos consecutivos. */
    private static final Pattern CBU = Pattern.compile("\\b\\d{22}\\b");

    /**
     * Tarjeta de credito: 13-19 digitos (con separadores opcionales). El
     * check de Luhn se aplica despues del match para evitar falsos positivos
     * con numeros aleatorios.
     */
    private static final Pattern CREDIT_CARD_CANDIDATE = Pattern.compile(
            "\\b(?:\\d[ -]?){12,18}\\d\\b"
    );

    /** Email RFC 5321 simplificado: suficiente para detectar y enmascarar. */
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    /**
     * Telefono AR: soporta varias formas comunes incluyendo +54, 0054, codigo
     * de area de 2-4 digitos y el 9 de celulares. Se queda con coincidencias
     * de al menos 10 digitos efectivos para evitar matchear numeros chicos.
     */
    private static final Pattern PHONE_AR = Pattern.compile(
            "(?:\\+54|0054)[\\s-]?(?:9[\\s-]?)?\\d{2,4}[\\s-]?\\d{6,8}\\b"
                    + "|\\b0\\d{2,4}[\\s-]?\\d{6,8}\\b"
                    + "|\\b15\\s?\\d{6,8}\\b"
    );

    /**
     * Resultado de la pasada de sanitizacion. {@code sanitized} es el texto
     * original con todas las coincidencias reemplazadas por placeholders
     * legibles. {@code detected} se completa con cada tipo que matcheo al
     * menos una vez (sin duplicados).
     */
    public record PiiScanResult(String original, String sanitized, Set<ChatbotPiiType> detected) {
        public boolean hasPii() {
            return detected != null && !detected.isEmpty();
        }
    }

    /**
     * Pasa el input por todos los detectores. Orden:
     * <ol>
     *   <li>Tarjeta (mas largo, evita que parte de la tarjeta matchee como DNI/CBU primero).</li>
     *   <li>CBU (22 digitos).</li>
     *   <li>CUIT (formato XX-XXXXXXXX-X o 11 digitos con prefijo valido).</li>
     *   <li>Telefono AR.</li>
     *   <li>Email.</li>
     *   <li>DNI (lo mas chico al final).</li>
     * </ol>
     */
    public PiiScanResult scan(String input) {
        if (input == null || input.isEmpty()) {
            return new PiiScanResult(input == null ? "" : "", input == null ? "" : "", EnumSet.noneOf(ChatbotPiiType.class));
        }
        Set<ChatbotPiiType> detected = EnumSet.noneOf(ChatbotPiiType.class);
        String result = input;

        result = replaceAllValidCreditCards(result, detected);
        result = replaceAll(result, CBU, "[CBU_REDACTADO]", detected, ChatbotPiiType.CBU);
        result = replaceAll(result, CUIT, "[CUIT_REDACTADO]", detected, ChatbotPiiType.CUIT);
        result = replaceAll(result, PHONE_AR, "[TELEFONO_REDACTADO]", detected, ChatbotPiiType.PHONE_AR);
        result = replaceAll(result, EMAIL, "[EMAIL_REDACTADO]", detected, ChatbotPiiType.EMAIL);
        result = replaceAll(result, DNI, "[DNI_REDACTADO]", detected, ChatbotPiiType.DNI);

        return new PiiScanResult(input, result, detected);
    }

    private String replaceAll(String text, Pattern pattern, String replacement,
                              Set<ChatbotPiiType> detected, ChatbotPiiType type) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return text;
        }
        // Si llegamos aca hubo al menos un match: marcamos el tipo y
        // reemplazamos todas las ocurrencias.
        detected.add(type);
        return m.replaceAll(replacement);
    }

    /**
     * Pasa el detector de tarjetas + valida Luhn para cada candidata. Si
     * pasa Luhn, reemplaza y registra. Si no, deja el texto intacto (era un
     * numero largo cualquiera). Operacion sobre StringBuilder para minimizar
     * allocs en mensajes largos.
     */
    private String replaceAllValidCreditCards(String text, Set<ChatbotPiiType> detected) {
        Matcher m = CREDIT_CARD_CANDIDATE.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        m.reset();
        while (m.find()) {
            String candidate = m.group();
            String digits = candidate.replaceAll("[^0-9]", "");
            if (digits.length() >= 13 && digits.length() <= 19 && luhnValid(digits)) {
                out.append(text, cursor, m.start());
                out.append("[TARJETA_REDACTADA]");
                cursor = m.end();
                detected.add(ChatbotPiiType.CREDIT_CARD);
            }
        }
        if (cursor == 0) {
            // No hubo tarjetas validas; devolvemos el original sin copias.
            return text;
        }
        out.append(text, cursor, text.length());
        return out.toString();
    }

    /**
     * Algoritmo de Luhn: valida checksum tipico de tarjetas de credito.
     * Implementacion clasica en O(n) sin allocs.
     */
    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
