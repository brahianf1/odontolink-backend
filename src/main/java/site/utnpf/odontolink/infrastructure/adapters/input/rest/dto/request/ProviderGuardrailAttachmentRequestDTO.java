package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.Min;

/**
 * Payload para actualizar la intencion de attach/priority de un
 * ProviderGuardrail. La fuente de verdad sigue siendo el proveedor; el
 * cambio se propaga en el proximo publish del agente.
 */
public class ProviderGuardrailAttachmentRequestDTO {

    private boolean attached;

    @Min(0)
    private int priority = 100;

    public boolean isAttached() { return attached; }
    public void setAttached(boolean attached) { this.attached = attached; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
