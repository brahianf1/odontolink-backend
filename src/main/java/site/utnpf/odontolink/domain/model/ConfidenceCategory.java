package site.utnpf.odontolink.domain.model;

/**
 * Categoria del indicador de confianza del chatbot (RF34).
 *
 * <p>El indicador de confianza se expresa como una de cuatro categorias
 * discretas en lugar de un porcentaje opaco. La decision esta justificada
 * en el PoC de mayo 2026: un porcentaje numerico depende fuertemente del
 * algoritmo de scoring del proveedor (DigitalOcean Gradient), tiene varianza
 * incompatible con la calidad real de la respuesta, y no es accionable para
 * el paciente. Las categorias dan al usuario una guia clara de que hacer
 * con la respuesta: confiar, verificar, o ignorar.
 *
 * <p>Precedentes industriales que sustentan el enfoque categorico:
 * <ul>
 *   <li>Perplexity AI: muestra citas, no porcentaje.</li>
 *   <li>Vertex AI: removio confidenceScore en Gemini 2.5+.</li>
 *   <li>IBM Watson Discovery: trabaja con niveles discretos.</li>
 * </ul>
 */
public enum ConfidenceCategory {

    /**
     * La respuesta esta solidamente respaldada por documentos institucionales
     * de la KB. El paciente puede confiar en el contenido sin verificacion
     * adicional inmediata.
     */
    OFFICIAL,

    /**
     * La respuesta se apoya en chunks de la KB pero con senales debiles (por
     * relevancia o cobertura). El paciente puede usarla pero conviene
     * confirmar si la consulta es importante.
     */
    PARTIAL,

    /**
     * El RAG no se activo: la respuesta proviene del conocimiento general del
     * modelo. No esta respaldada por documentos institucionales.
     */
    GENERAL,

    /**
     * La consulta esta fuera del ambito del chatbot. El chatbot rechazo o
     * desvio (educadamente) y el paciente debe buscar la informacion en
     * otra parte.
     */
    OUT_OF_SCOPE;
}
