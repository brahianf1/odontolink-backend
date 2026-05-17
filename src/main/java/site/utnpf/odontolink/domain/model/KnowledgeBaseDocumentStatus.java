package site.utnpf.odontolink.domain.model;

/**
 * Estado del ciclo de vida de un {@link KnowledgeBaseDocument} dentro del
 * pipeline asincronico Upload -> Register -> Indexing -> Indexed (RF33).
 *
 * <p>El estado avanza monotonicamente salvo en {@link #FAILED}, que puede
 * surgir desde cualquier estadio previo y deja al documento esperando una
 * accion del administrador (reintentar via {@code refreshIndexingStatus} o
 * borrar).
 */
public enum KnowledgeBaseDocumentStatus {

    /**
     * El documento existe en BD pero todavia no se subio al bucket. Estado
     * raramente persistido: solo si el caller decide separar el insert del
     * upload por algun motivo (p. ej. encolar y subir en batch). En el flujo
     * sincronico actual el documento pasa directo de aqui a {@link #UPLOADED}.
     */
    PENDING_UPLOAD,

    /**
     * Bytes subidos al bucket Spaces; aun no se notifico al proveedor de IA.
     */
    UPLOADED,

    /**
     * El proveedor reconocio el archivo como un data source de la KB pero
     * todavia no se inicio la indexacion. Estado breve en la practica.
     */
    REGISTERED,

    /**
     * Indexing job disparado y en progreso en el proveedor.
     */
    INDEXING,

    /**
     * Indexing job terminado con exito. El contenido ya esta disponible para
     * los retrievals que haga el agente.
     */
    INDEXED,

    /**
     * Algun paso del pipeline fallo. El campo {@code errorMessage} del
     * documento contiene el detalle para diagnostico. Requiere accion del
     * administrador (reintentar o eliminar).
     */
    FAILED
}
