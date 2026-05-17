package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.KnowledgeBaseDocument;
import site.utnpf.odontolink.domain.model.KnowledgeBaseDocumentStatus;
import site.utnpf.odontolink.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para los documentos de la Knowledge Base administrada
 * (RF33). Mantiene el espejo local de los data sources del proveedor.
 */
public interface KnowledgeBaseDocumentRepository {

    List<KnowledgeBaseDocument> findAllOrderByCreatedAtDesc();

    /**
     * Devuelve una pagina de documentos ordenados por {@code createdAt}
     * descendente, opcionalmente filtrados por {@code status}. Si
     * {@code status} es {@code null} no se filtra.
     */
    PageResult<KnowledgeBaseDocument> findPaged(KnowledgeBaseDocumentStatus status, int page, int size);

    Optional<KnowledgeBaseDocument> findById(Long id);

    Optional<KnowledgeBaseDocument> findByProviderDataSourceId(String providerDataSourceId);

    List<KnowledgeBaseDocument> findByStatusIn(List<KnowledgeBaseDocumentStatus> statuses);

    KnowledgeBaseDocument save(KnowledgeBaseDocument document);

    void deleteById(Long id);
}
