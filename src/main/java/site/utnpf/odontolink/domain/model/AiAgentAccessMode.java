package site.utnpf.odontolink.domain.model;

/**
 * Modo de acceso del chatbot institucional (RF29).
 *
 * <p>El administrador controla en runtime quien puede conversar con el bot:
 * <ul>
 *   <li>{@link #PUBLIC}: accesible para usuarios anonimos (sin token) y autenticados.</li>
 *   <li>{@link #PRIVATE}: requiere autenticacion y que el rol del usuario este en
 *       {@code allowedRoles}.</li>
 *   <li>{@link #DISABLED}: el endpoint responde 403 a todos. Util para apagar el
 *       chatbot sin redeploy cuando hay incidentes con el proveedor o se detectan
 *       abusos.</li>
 * </ul>
 *
 * <p>El cambio entre modos es inmediato: la siguiente request consulta la
 * configuracion vigente y aplica la regla. Sesiones en curso en modo PUBLIC que
 * pasan a DISABLED reciben 403 en su proximo mensaje (comportamiento esperado).
 */
public enum AiAgentAccessMode {
    PUBLIC,
    PRIVATE,
    DISABLED
}
