package site.utnpf.odontolink.domain.model;

/**
 * Estrategia de recuperacion (retrieval) que utiliza el agente IA al consultar
 * la Knowledge Base durante el flujo RAG (RF33).
 *
 * <p>Cada constante mapea 1:1 a uno de los modos soportados por la API del
 * proveedor (DigitalOcean Gradient en este momento), pero se modela en el
 * dominio para que la capa de aplicacion no conozca el prefijo
 * {@code RETRIEVAL_METHOD_*} propio del proveedor. El adapter es el unico
 * responsable de hacer el mapeo concreto.
 *
 * <ul>
 *   <li>{@link #REWRITE}: reescribe la query del usuario para mejorar el recall.</li>
 *   <li>{@link #STEP_BACK}: genera una pregunta mas general antes de recuperar.</li>
 *   <li>{@link #SUB_QUERIES}: descompone la pregunta en sub-consultas paralelas.</li>
 *   <li>{@link #NONE}: usa la query original sin transformacion.</li>
 * </ul>
 */
public enum AiRetrievalMethod {
    REWRITE,
    STEP_BACK,
    SUB_QUERIES,
    NONE
}
