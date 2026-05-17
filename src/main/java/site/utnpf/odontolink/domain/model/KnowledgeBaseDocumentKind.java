package site.utnpf.odontolink.domain.model;

/**
 * Tipo de documento dentro de la Knowledge Base del agente IA (RF33).
 *
 * <p>La distincion se mantiene en el dominio porque, aunque el flujo de upload
 * al bucket de storage es identico (ambos terminan como objetos cuyo
 * {@code stored_object_key} se registra como {@code spaces_data_source} en el
 * proveedor), la presentacion al administrador difiere: las FAQs llevan el
 * contenido textual editable inline, los archivos solo nombre + tamaño.
 */
public enum KnowledgeBaseDocumentKind {

    /**
     * FAQ creada como texto plano desde el panel. El contenido vive primero
     * como {@code inlineContent} en BD y se serializa a TXT al subirlo al
     * bucket, evitando que el administrador tenga que editar archivos para
     * actualizar una respuesta.
     */
    FAQ_TEXT,

    /**
     * Archivo binario subido por el administrador (PDF, DOCX, TXT, etc.).
     * No tenemos contenido inline; solo metadatos y el {@code storedObjectKey}.
     */
    UPLOADED_FILE
}
