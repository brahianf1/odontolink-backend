package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;

/**
 * Guardrail nativo del proveedor LLM, espejado localmente para gestionar
 * attach/detach con el agente (RF31).
 *
 * <p><b>Distincion clave</b>: a diferencia de {@link AgentPolicyRule} (que es
 * texto que se concatena al system prompt), un {@code ProviderGuardrail} es
 * un <strong>recurso del proveedor</strong> (procesador binario que filtra
 * inputs/outputs). En DigitalOcean Gradient son tipos pre-built fijos
 * (jailbreak / sensitive_data / content_moderation). Su configuracion fina
 * (categorias de Presidio, default_response, etc.) NO se expone via API; solo
 * en el dashboard del proveedor. Nuestro backend gestiona attach/detach +
 * priority + un espejo local de la metadata.
 *
 * <p>Modelo agnostico al proveedor: si manana migramos a Anthropic/Bedrock,
 * mapeamos los nuevos tipos al enum {@link ProviderGuardrailType#OTHER} y
 * reutilizamos el resto del flujo.
 *
 * <p>El admin gestiona el catalogo local desde el panel: la fuente de verdad
 * de "que esta vinculado al agente publicado" sigue siendo DO; nuestro
 * espejo local guarda la <strong>intencion del admin</strong> ({@code
 * attached: true}) que se reconcilia con DO en el proximo {@code publish()}.
 */
public class ProviderGuardrail {

    private Long id;
    /** UUID del guardrail en el proveedor (e.g., DO). Inmutable. */
    private String providerGuardrailUuid;
    /** Tipo segun la taxonomia agnostica al proveedor. */
    private ProviderGuardrailType type;
    /** Nombre legible (suele venir del proveedor o lo edita el admin localmente). */
    private String displayName;
    /** Descripcion legible para el admin. Solo informativa. */
    private String description;
    /** Si {@code true}, debe estar vinculado al agente al proximo publish. */
    private boolean attached;
    /** Prioridad (orden de aplicacion); valores menores se aplican primero. */
    private int priority;
    /**
     * Respuesta default que el proveedor mostrara cuando el guardrail se
     * dispara. Espejo de solo lectura (DO no permite editarla via API).
     */
    private String defaultResponse;
    private Instant createdAt;
    private Instant updatedAt;

    public ProviderGuardrail() {
    }

    public ProviderGuardrail(Long id,
                             String providerGuardrailUuid,
                             ProviderGuardrailType type,
                             String displayName,
                             String description,
                             boolean attached,
                             int priority,
                             String defaultResponse,
                             Instant createdAt,
                             Instant updatedAt) {
        this.id = id;
        this.providerGuardrailUuid = providerGuardrailUuid;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.attached = attached;
        this.priority = priority;
        this.defaultResponse = defaultResponse;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Crea un nuevo espejo local a partir de la metadata que el proveedor
     * reporta. Lo usa el flujo de "sincronizar disponibles" cuando el admin
     * pulsa "Refrescar desde el proveedor".
     */
    public static ProviderGuardrail fromProviderMetadata(String providerGuardrailUuid,
                                                         ProviderGuardrailType type,
                                                         String displayName,
                                                         String description,
                                                         String defaultResponse) {
        validateUuid(providerGuardrailUuid);
        Instant now = Instant.now();
        return new ProviderGuardrail(
                null,
                providerGuardrailUuid,
                type == null ? ProviderGuardrailType.OTHER : type,
                displayName,
                description,
                false,
                100,
                defaultResponse,
                now,
                now);
    }

    /** Actualiza la intencion de attach + priority sin tocar la metadata. */
    public void setAttachmentIntent(boolean attached, int priority) {
        if (priority < 0) {
            throw new InvalidBusinessRuleException("La prioridad no puede ser negativa.");
        }
        this.attached = attached;
        this.priority = priority;
        this.updatedAt = Instant.now();
    }

    /** Refresca la metadata (descriptiva) sin tocar la intencion del admin. */
    public void refreshMetadataFromProvider(String displayName,
                                            String description,
                                            String defaultResponse) {
        this.displayName = displayName;
        this.description = description;
        this.defaultResponse = defaultResponse;
        this.updatedAt = Instant.now();
    }

    private static void validateUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new InvalidBusinessRuleException("providerGuardrailUuid es obligatorio.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderGuardrailUuid() {
        return providerGuardrailUuid;
    }

    public ProviderGuardrailType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAttached() {
        return attached;
    }

    public int getPriority() {
        return priority;
    }

    public String getDefaultResponse() {
        return defaultResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
