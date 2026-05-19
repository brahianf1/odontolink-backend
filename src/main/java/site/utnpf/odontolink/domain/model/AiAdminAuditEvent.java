package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Evento de auditoria de operaciones de gobernanza del agente IA (RF31).
 *
 * <p>Alcance acotado por decision del usuario: solo se registran eventos
 * de ciclo de vida (PUBLISH, ROLLBACK) y cambios de
 * {@link AiGovernancePolicy}. Las ediciones rutinarias de configuracion,
 * guardrails individuales o documentos NO generan filas en esta tabla;
 * el versionado captura el estado del agente al momento del publish.
 *
 * <p>{@code details} guarda un texto libre con la informacion relevante
 * del evento (p. ej. requisitos faltantes, version desde la que se hace
 * rollback). No es JSON estructurado por simplicidad de la primera
 * iteracion: un campo searchable es suficiente para los pocos eventos que
 * esta tabla recibira.
 */
public class AiAdminAuditEvent {

    public enum Type {
        AGENT_PUBLISH,
        AGENT_PUBLISH_FAILED,
        AGENT_ROLLBACK,
        GOVERNANCE_POLICY_UPDATED,
        /**
         * Publish exitoso pero la sincronizacion de uno o mas guardrails
         * nativos del proveedor fallo. El agente quedo publicado con
         * instruction y parametros pero el set de guardrails attached en DO
         * puede no coincidir con el intent local. {@code details} guarda los
         * UUIDs fallidos para que el admin pueda investigar.
         */
        PROVIDER_GUARDRAIL_SYNC_PARTIAL_FAILURE
    }

    private Long id;
    private Type type;
    private Long actorUserId;
    /** Numero de version asociado al evento (null si no aplica). */
    private Integer relatedVersionNumber;
    /** Indica si el evento se realizo con override (relevante en publish). */
    private boolean withOverride;
    private String details;
    private Instant occurredAt;

    public AiAdminAuditEvent() {
    }

    public AiAdminAuditEvent(Long id,
                             Type type,
                             Long actorUserId,
                             Integer relatedVersionNumber,
                             boolean withOverride,
                             String details,
                             Instant occurredAt) {
        this.id = id;
        this.type = type;
        this.actorUserId = actorUserId;
        this.relatedVersionNumber = relatedVersionNumber;
        this.withOverride = withOverride;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public static AiAdminAuditEvent of(Type type, Long actorUserId,
                                       Integer relatedVersionNumber,
                                       boolean withOverride, String details) {
        return new AiAdminAuditEvent(null, type, actorUserId, relatedVersionNumber,
                withOverride, details, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Integer getRelatedVersionNumber() {
        return relatedVersionNumber;
    }

    public boolean isWithOverride() {
        return withOverride;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
