package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.out.IKnowledgeBaseProviderPort.IndexingJobSnapshot;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;

import java.util.List;

/**
 * Puerto de entrada para la administracion de la Knowledge Base (RF33).
 *
 * <p>Modela el ciclo de vida completo de un documento desde la perspectiva
 * del administrador: alta (FAQ o archivo), lectura, refresco de estado y
 * baja. La indexacion se gatilla de forma transparente tras cada alta/baja.
 */
public interface IKnowledgeBaseAdminUseCase {

    List<KnowledgeBaseDocument> listDocuments();

    KnowledgeBaseDocument getDocument(Long id);

    /**
     * Crea un documento FAQ a partir de texto plano. El servicio se encarga
     * de envolverlo como TXT, subirlo al bucket, registrarlo en el proveedor
     * y disparar la indexacion.
     */
    KnowledgeBaseDocument addFaqDocument(String title, String content);

    /**
     * Crea un documento a partir de un archivo subido. {@code contentType}
     * se valida contra la whitelist de tipos soportados por la KB.
     */
    KnowledgeBaseDocument addFileDocument(String title,
                                          String originalFileName,
                                          byte[] content,
                                          String contentType);

    /**
     * Edita la metadata y, opcionalmente, el contenido de una FAQ.
     *
     * @param id        identificador local del documento.
     * @param title     nuevo titulo (obligatorio).
     * @param content   nuevo contenido para FAQs. Para archivos subidos debe
     *                  llegar {@code null}; mandarlo genera 422 porque los
     *                  binarios no se editan inline. Para FAQs, si llega
     *                  distinto del actual, el servicio re-sube los bytes al
     *                  bucket y dispara reindex del data source asociado.
     */
    KnowledgeBaseDocument updateDocument(Long id, String title, String content);

    /**
     * Borra el documento, su binario del bucket y su data source remoto. La
     * operacion es idempotente: si el documento ya no existe en alguno de
     * los tres lados, se sigue adelante.
     */
    void deleteDocument(Long id);

    /**
     * Dispara un indexing job manual. Es un fallback de troubleshooting: el
     * flujo normal indexa automaticamente tras cada alta o baja.
     */
    IndexingJobSnapshot triggerReindex();

    /**
     * Consulta al proveedor el estado del ultimo indexing job asociado al
     * documento y refresca el estado local en consecuencia.
     */
    KnowledgeBaseDocument refreshIndexingStatus(Long id);

    /**
     * Consulta al proveedor el estado de un indexing job por su UUID. No
     * toca documentos locales: lo usa el frontend para pollear el progreso
     * de un reindex global (POST /reindex) sin tener que iterar por documento.
     */
    IndexingJobSnapshot getIndexingJob(String jobId);

    /**
     * Resultado de la descarga de un documento: incluye el binario y la
     * metadata necesaria para que el controller construya el response con
     * los headers correctos.
     */
    record DocumentDownload(byte[] content, String contentType, String fileName) {
    }

    /**
     * Descarga el binario asociado al documento. Para FAQs devuelve el
     * {@code inlineContent} serializado como TXT UTF-8. Para archivos
     * subidos, lee el objeto del bucket Spaces correspondiente.
     */
    DocumentDownload downloadDocument(Long id);

    /**
     * Lista paginada de documentos. Soporta filtro opcional por estado para
     * que el frontend pueda mostrar pestanias (p. ej. "fallidos", "indexando").
     */
    site.utnpf.odontolink.domain.model.PageResult<KnowledgeBaseDocument> listDocumentsPaged(
            site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus status,
            int page,
            int size);
}
