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
}
