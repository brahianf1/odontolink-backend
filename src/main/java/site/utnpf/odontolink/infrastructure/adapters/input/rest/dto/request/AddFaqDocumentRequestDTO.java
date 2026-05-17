package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload JSON para alta de FAQ via POST
 * {@code /api/admin/ai-agent/knowledge-base/documents/faq}.
 */
public class AddFaqDocumentRequestDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
