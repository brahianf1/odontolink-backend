package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload del PUT a {@code /api/admin/ai-agent/knowledge-base/documents/{id}}.
 *
 * <p>El {@code title} es obligatorio. El {@code content} solo se acepta para
 * documentos {@code FAQ_TEXT}; para archivos subidos viajara {@code null} (o
 * se ignora). Si se manda con un archivo, el servicio devuelve 422 con
 * {@code AI_KB_UNSUPPORTED_TYPE}.
 */
public class UpdateKnowledgeBaseDocumentRequestDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    /**
     * Nuevo contenido inline (solo aplicable a FAQs). Si llega {@code null}
     * o igual al actual, no se re-sube ni se re-indexa.
     */
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
