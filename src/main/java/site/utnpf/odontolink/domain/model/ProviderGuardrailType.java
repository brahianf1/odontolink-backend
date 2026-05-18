package site.utnpf.odontolink.domain.model;

/**
 * Tipo de guardrail nativo del proveedor LLM (RF31).
 *
 * <p>Mapea 1:1 con los tipos de guardrail que DigitalOcean Gradient expone
 * en su API hoy ({@code GUARDRAIL_TYPE_JAILBREAK}, {@code SENSITIVE_DATA},
 * {@code CONTENT_MODERATION}). Si en el futuro DO agrega tipos nuevos o
 * migramos a otro proveedor, lo mapeamos via {@link #OTHER} sin romper el
 * dominio.
 *
 * <p><b>Importante</b>: estos tipos son los que el proveedor implementa como
 * procesadores binarios. Su configuracion fina (categorias del Sensitive
 * Data Detection, default_response, etc.) NO se expone via API publica de
 * DO al dia de hoy — solo es editable en el dashboard del proveedor.
 * Nuestro backend solo gestiona attach/detach + priority.
 */
public enum ProviderGuardrailType {
    /** Detector de prompt injection / intentos de jailbreak. */
    JAILBREAK,
    /** Filtro de datos sensibles (PII) en input/output (Presidio en DO). */
    SENSITIVE_DATA,
    /** Filtro de contenido toxic / NSFW. */
    CONTENT_MODERATION,
    /** Tipo desconocido (forward-compatibility con nuevos tipos del proveedor). */
    OTHER;

    /** Mapea desde el formato wire del proveedor DO. */
    public static ProviderGuardrailType fromProviderString(String raw) {
        if (raw == null) {
            return OTHER;
        }
        String stripped = raw.startsWith("GUARDRAIL_TYPE_")
                ? raw.substring("GUARDRAIL_TYPE_".length())
                : raw;
        try {
            return ProviderGuardrailType.valueOf(stripped.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
