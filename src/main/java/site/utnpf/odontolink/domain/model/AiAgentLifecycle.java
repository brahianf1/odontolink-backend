package site.utnpf.odontolink.domain.model;

/**
 * Estado del ciclo de vida del agente IA (RF31).
 *
 * <p>Es la herramienta de mitigacion principal frente a errores del
 * administrador: las ediciones nunca afectan al paciente de forma directa.
 * El admin trabaja en DRAFT (no sincronizado con el proveedor) y solo al
 * presionar Publicar se aplican los checks de gobernanza y se empuja la
 * configuracion al LLM externo.
 *
 * <ul>
 *   <li>{@link #UNCONFIGURED}: no hay fila en BD todavia. Estado virtual
 *       que el servicio infiere al no encontrar el singleton; nunca se
 *       persiste. El primer alta crea la fila directamente en DRAFT.</li>
 *   <li>{@link #DRAFT}: existe la configuracion pero el agente no sirve
 *       respuestas. Cualquier edicion deja al agregado en DRAFT.</li>
 *   <li>{@link #PUBLISHED}: configuracion sincronizada con el proveedor.
 *       El chatbot del paciente responde con esta configuracion. Una
 *       edicion posterior lo regresa a DRAFT hasta el proximo publish.</li>
 * </ul>
 */
public enum AiAgentLifecycle {
    UNCONFIGURED,
    DRAFT,
    PUBLISHED
}
