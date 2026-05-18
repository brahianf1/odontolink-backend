package site.utnpf.odontolink.domain.model;

/**
 * Politica de manejo de PII (Personal Identifiable Information) detectada en
 * los mensajes del usuario antes de enviarlos al proveedor LLM (RF31, RF32).
 *
 * <p>La defensa en profundidad exige no confiar solo en los guardrails del
 * proveedor: filtramos localmente DNI, CUIT, CBU, tarjetas, email y telefonos
 * argentinos antes de que el texto salga del backend. El admin decide que hacer
 * cuando se detecta:
 * <ul>
 *   <li>{@link #BLOCK}: no se envia el mensaje al proveedor. El bot responde
 *       con un texto educativo pidiendo al usuario que reformule sin datos
 *       sensibles. El mensaje original tampoco se persiste en el rolling
 *       buffer (evita guardar PII).</li>
 *   <li>{@link #ANONYMIZE}: las coincidencias se reemplazan por placeholders
 *       (p. ej. {@code [DNI_REDACTADO]}) y el mensaje sanitizado se envia al
 *       proveedor y se persiste. UX mas fluida pero deja la mascara en BD.</li>
 * </ul>
 *
 * <p>El default en {@code createNew()} es {@link #BLOCK} porque privilegia el
 * compliance: nunca se manda PII al proveedor por accidente.
 */
public enum AiPiiPolicy {
    BLOCK,
    ANONYMIZE
}
