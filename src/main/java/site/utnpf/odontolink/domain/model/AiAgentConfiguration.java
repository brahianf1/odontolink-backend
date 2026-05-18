package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Configuracion del agente IA conversacional (RF29, RF31, RF32, RF34).
 *
 * <p>Singleton: hay a lo sumo una fila en BD. Si no existe, el sistema
 * reporta {@link AiAgentLifecycle#UNCONFIGURED} (estado virtual, no
 * persistido). El primer POST del admin crea la fila directamente en
 * {@link AiAgentLifecycle#DRAFT}.
 *
 * <p>Importante: este agregado NO embebe contenido por defecto. El admin
 * es el responsable funcional de proveer displayName, systemPromptCore,
 * welcomeMessage y los parametros numericos. La mitigacion contra
 * publicaciones incompletas vive en {@link AiGovernancePolicy} y en el
 * flujo {@code POST /publish}.
 *
 * <p>A partir de RF29/RF34 incorpora ademas la configuracion del chatbot
 * institucional (acceso publico/privado, politica de PII, rolling buffer,
 * rate limits, URL de invocacion cacheada, banner de emergencia). Cualquier
 * edicion exitosa pasa lifecycle a DRAFT: los cambios no afectan al paciente
 * hasta el siguiente publish exitoso.
 */
public class AiAgentConfiguration {

    /** Identificador unico de la fila singleton. */
    public static final Long SINGLETON_ID = 1L;

    /** Banner usado cuando el detector local marca emergencia y el admin no override. */
    public static final String DEFAULT_EMERGENCY_BANNER =
            "*** ATENCION: si esto es una emergencia, comunicate inmediatamente con la clinica o " +
                    "concurri a una guardia odontologica. ***\n\n";

    private Long id;
    private String displayName;
    private String systemPromptCore;
    private String welcomeMessage;
    private BigDecimal temperature;
    private BigDecimal topP;
    private int maxTokens;
    private int k;
    private AiRetrievalMethod retrievalMethod;
    /** Estado del ciclo de vida; arranca en DRAFT al crear la fila. */
    private AiAgentLifecycle lifecycle;
    /** UUID del agente en el proveedor; null hasta el primer publish exitoso. */
    private String providerAgentId;
    /** Instante del ultimo publish exitoso. */
    private Instant providerSyncedAt;
    /** Detalle del ultimo error de sync; null si el ultimo sync fue exitoso. */
    private String lastSyncError;
    private Instant updatedAt;

    // -- Configuracion del chatbot (RF29/RF31/RF32/RF34) ------------------

    /** Modo de acceso publico/privado/desactivado. Default: DISABLED (fail-safe). */
    private AiAgentAccessMode accessMode;
    /** Roles permitidos cuando accessMode==PRIVATE. Vacio en PUBLIC/DISABLED. */
    private Set<Role> allowedRoles;
    /** Politica al detectar PII en el mensaje del usuario. Default: BLOCK. */
    private AiPiiPolicy piiPolicy;
    /** Cap del rolling buffer de mensajes por sesion. Default 20, rango [4, 50]. */
    private int conversationBufferSize;
    /** Mensajes/hora permitidos por IP anonima. Default 20, rango [1, 1000]. */
    private int rateLimitAnonymousPerHour;
    /** Mensajes/hora permitidos por usuario autenticado. Default 60, rango [1, 5000]. */
    private int rateLimitAuthenticatedPerHour;
    /** URL de invocacion del agente (chat completions). Descubierta+cacheada. */
    private String agentInvocationUrl;
    /** Texto antepuesto al reply cuando el detector local marca emergencia. */
    private String emergencyBannerText;
    /**
     * Si {@code true}, el agente devuelve las citas (referencias a documentos
     * de la KB) inline en cada respuesta. Default {@code false} porque el
     * caso clinico prefiere respuestas limpias para el paciente. Se sincroniza
     * via {@code provide_citations} al hacer publish.
     */
    private boolean provideCitations;

    public AiAgentConfiguration() {
    }

    public AiAgentConfiguration(Long id,
                                String displayName,
                                String systemPromptCore,
                                String welcomeMessage,
                                BigDecimal temperature,
                                BigDecimal topP,
                                int maxTokens,
                                int k,
                                AiRetrievalMethod retrievalMethod,
                                AiAgentLifecycle lifecycle,
                                String providerAgentId,
                                Instant providerSyncedAt,
                                String lastSyncError,
                                Instant updatedAt,
                                AiAgentAccessMode accessMode,
                                Set<Role> allowedRoles,
                                AiPiiPolicy piiPolicy,
                                int conversationBufferSize,
                                int rateLimitAnonymousPerHour,
                                int rateLimitAuthenticatedPerHour,
                                String agentInvocationUrl,
                                String emergencyBannerText,
                                boolean provideCitations) {
        this.id = id;
        this.displayName = displayName;
        this.systemPromptCore = systemPromptCore;
        this.welcomeMessage = welcomeMessage;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.k = k;
        this.retrievalMethod = retrievalMethod;
        this.lifecycle = lifecycle;
        this.providerAgentId = providerAgentId;
        this.providerSyncedAt = providerSyncedAt;
        this.lastSyncError = lastSyncError;
        this.updatedAt = updatedAt;
        this.accessMode = accessMode;
        this.allowedRoles = copyRolesSafely(allowedRoles);
        this.piiPolicy = piiPolicy;
        this.conversationBufferSize = conversationBufferSize;
        this.rateLimitAnonymousPerHour = rateLimitAnonymousPerHour;
        this.rateLimitAuthenticatedPerHour = rateLimitAuthenticatedPerHour;
        this.agentInvocationUrl = agentInvocationUrl;
        this.emergencyBannerText = emergencyBannerText;
        this.provideCitations = provideCitations;
    }

    /**
     * Copia segura de roles a un {@link EnumSet}. Centralizamos el chequeo
     * porque {@link EnumSet#copyOf(java.util.Collection)} lanza
     * {@link IllegalArgumentException} cuando recibe una coleccion vacia
     * (no puede inferir el tipo del enum sin elementos). El contrato del
     * dominio acepta listas vacias para accessMode != PRIVATE; sin este
     * helper, el frontend que envia {@code allowedRoles: []} explicitamente
     * rompe el flujo con un 500.
     */
    private static EnumSet<Role> copyRolesSafely(Set<Role> input) {
        if (input == null || input.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }
        return EnumSet.copyOf(input);
    }

    /**
     * Factory para el primer alta. La fila arranca en DRAFT con los valores
     * que el admin envia, y los campos de chatbot con defaults conservadores
     * (DISABLED + BLOCK + buffer 20 + rate limits razonables).
     */
    public static AiAgentConfiguration createNew(String displayName,
                                                 String systemPromptCore,
                                                 String welcomeMessage,
                                                 BigDecimal temperature,
                                                 BigDecimal topP,
                                                 int maxTokens,
                                                 int k,
                                                 AiRetrievalMethod retrievalMethod) {
        AiAgentConfiguration config = new AiAgentConfiguration(
                SINGLETON_ID, null, null, null, null, null, 0, 0, null,
                AiAgentLifecycle.DRAFT, null, null, null, null,
                AiAgentAccessMode.DISABLED, EnumSet.noneOf(Role.class), AiPiiPolicy.BLOCK,
                20, 20, 60, null, DEFAULT_EMERGENCY_BANNER, false
        );
        config.apply(displayName, systemPromptCore, welcomeMessage,
                temperature, topP, maxTokens, k, retrievalMethod);
        return config;
    }

    /**
     * Aplica el comando de actualizacion clasico (campos del agente IA).
     * Cualquier edicion exitosa revierte el lifecycle a DRAFT.
     */
    public void apply(String displayName,
                      String systemPromptCore,
                      String welcomeMessage,
                      BigDecimal temperature,
                      BigDecimal topP,
                      int maxTokens,
                      int k,
                      AiRetrievalMethod retrievalMethod) {
        validateNonBlank(displayName, "displayName");
        validateNonBlank(systemPromptCore, "systemPromptCore");
        validateRange(temperature, "temperature", BigDecimal.ZERO, BigDecimal.ONE);
        validateRange(topP, "topP", BigDecimal.ZERO, BigDecimal.ONE);
        validateIntRange(maxTokens, "maxTokens", 1, 512);
        validateIntRange(k, "k", 1, 50);
        if (retrievalMethod == null) {
            throw new InvalidBusinessRuleException("retrievalMethod es obligatorio.");
        }

        this.displayName = displayName.trim();
        this.systemPromptCore = systemPromptCore;
        this.welcomeMessage = welcomeMessage;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.k = k;
        this.retrievalMethod = retrievalMethod;
        this.lifecycle = AiAgentLifecycle.DRAFT;
        this.updatedAt = Instant.now();
    }

    /**
     * Aplica el bloque de configuracion del chatbot. Tambien revierte el
     * lifecycle a DRAFT para que el cambio se haga visible solo tras un
     * publish explicito (excepcion: el cache de {@code agentInvocationUrl} se
     * actualiza por separado con {@link #cacheAgentInvocationUrl(String)}
     * porque no es una edicion del admin sino un descubrimiento interno).
     */
    public void applyChatbotConfig(AiAgentAccessMode accessMode,
                                   Set<Role> allowedRoles,
                                   AiPiiPolicy piiPolicy,
                                   int conversationBufferSize,
                                   int rateLimitAnonymousPerHour,
                                   int rateLimitAuthenticatedPerHour,
                                   String emergencyBannerText,
                                   boolean provideCitations) {
        if (accessMode == null) {
            throw new InvalidBusinessRuleException("accessMode es obligatorio.");
        }
        if (piiPolicy == null) {
            throw new InvalidBusinessRuleException("piiPolicy es obligatorio.");
        }
        validateIntRange(conversationBufferSize, "conversationBufferSize", 4, 50);
        validateIntRange(rateLimitAnonymousPerHour, "rateLimitAnonymousPerHour", 1, 1000);
        validateIntRange(rateLimitAuthenticatedPerHour, "rateLimitAuthenticatedPerHour", 1, 5000);
        // Roles solo aplican en PRIVATE; los aceptamos vacios en PUBLIC/DISABLED
        // pero exigimos no-vacio en PRIVATE al momento del publish (no aqui).
        if (emergencyBannerText == null || emergencyBannerText.isBlank()) {
            throw new InvalidBusinessRuleException("emergencyBannerText no puede ser vacio.");
        }

        this.accessMode = accessMode;
        this.allowedRoles = copyRolesSafely(allowedRoles);
        this.piiPolicy = piiPolicy;
        this.conversationBufferSize = conversationBufferSize;
        this.rateLimitAnonymousPerHour = rateLimitAnonymousPerHour;
        this.rateLimitAuthenticatedPerHour = rateLimitAuthenticatedPerHour;
        this.emergencyBannerText = emergencyBannerText;
        this.provideCitations = provideCitations;
        this.lifecycle = AiAgentLifecycle.DRAFT;
        this.updatedAt = Instant.now();
    }

    /**
     * Persiste la URL de invocacion descubierta por el adapter. NO cambia el
     * lifecycle: es metadata interna, no una edicion del admin.
     */
    public void cacheAgentInvocationUrl(String url) {
        this.agentInvocationUrl = url;
    }

    /** Borra el cache de URL (admin endpoint para forzar redescubrimiento). */
    public void clearAgentInvocationUrlCache() {
        this.agentInvocationUrl = null;
    }

    /**
     * Construye la instruccion final que viaja al proveedor anteponiendo el
     * texto de las {@link AgentPolicyRule} activas al {@code systemPromptCore}.
     * La lista la calcula el servicio leyendo de
     * {@code ai_guardrails where active=true} (el nombre fisico de la tabla
     * se mantiene por compatibilidad de schema; ver
     * {@code AgentPolicyRuleEntity}).
     *
     * <p>Las {@link ProviderGuardrail} (guardrails nativos de DO) NO se
     * concatenan aqui: se sincronizan via attach/detach en {@code publish()}.
     */
    public String composeInstruction(List<AgentPolicyRule> activeRules) {
        StringBuilder sb = new StringBuilder();
        if (activeRules != null && !activeRules.isEmpty()) {
            sb.append("## Reglas estrictas de seguridad (obligatorias, no negociables)\n");
            int idx = 1;
            for (AgentPolicyRule rule : activeRules) {
                sb.append(idx++).append(". ").append(rule.getText()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("## Rol y comportamiento\n");
        sb.append(systemPromptCore == null ? "" : systemPromptCore);
        return sb.toString();
    }

    /**
     * Determina si un caller puede usar el chatbot dado el {@code accessMode}
     * actual. NO valida lifecycle ni rate limit: solo permisos de rol.
     *
     * @param authenticatedUserRole rol del caller, o {@code null} si es anonimo.
     */
    public boolean canBeUsedBy(Role authenticatedUserRole) {
        if (accessMode == AiAgentAccessMode.DISABLED) {
            return false;
        }
        if (accessMode == AiAgentAccessMode.PUBLIC) {
            return true;
        }
        // PRIVATE: requiere autenticacion + rol permitido
        return authenticatedUserRole != null
                && allowedRoles != null
                && allowedRoles.contains(authenticatedUserRole);
    }

    public void markPublished(String providerAgentId, Instant now) {
        if (providerAgentId != null && !providerAgentId.isBlank()) {
            this.providerAgentId = providerAgentId;
        }
        this.providerSyncedAt = now;
        this.lastSyncError = null;
        this.lifecycle = AiAgentLifecycle.PUBLISHED;
    }

    public void markPublishFailed(String reason) {
        this.lifecycle = AiAgentLifecycle.DRAFT;
        this.lastSyncError = reason;
    }

    public void markDraft() {
        this.lifecycle = AiAgentLifecycle.DRAFT;
    }

    private static void validateNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidBusinessRuleException("El campo '" + field + "' es obligatorio.");
        }
    }

    private static void validateRange(BigDecimal value, String field, BigDecimal min, BigDecimal max) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new InvalidBusinessRuleException(
                    "El campo '" + field + "' debe estar en el rango [" + min + ", " + max + "].");
        }
    }

    private static void validateIntRange(int value, String field, int min, int max) {
        if (value < min || value > max) {
            throw new InvalidBusinessRuleException(
                    "El campo '" + field + "' debe estar en el rango [" + min + ", " + max + "].");
        }
    }

    // Getters / setters --------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSystemPromptCore() {
        return systemPromptCore;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public BigDecimal getTopP() {
        return topP;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getK() {
        return k;
    }

    public AiRetrievalMethod getRetrievalMethod() {
        return retrievalMethod;
    }

    public AiAgentLifecycle getLifecycle() {
        return lifecycle;
    }

    public String getProviderAgentId() {
        return providerAgentId;
    }

    public Instant getProviderSyncedAt() {
        return providerSyncedAt;
    }

    public String getLastSyncError() {
        return lastSyncError;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public AiAgentAccessMode getAccessMode() {
        return accessMode;
    }

    public Set<Role> getAllowedRoles() {
        return allowedRoles == null ? Collections.emptySet() : Collections.unmodifiableSet(allowedRoles);
    }

    public AiPiiPolicy getPiiPolicy() {
        return piiPolicy;
    }

    public int getConversationBufferSize() {
        return conversationBufferSize;
    }

    public int getRateLimitAnonymousPerHour() {
        return rateLimitAnonymousPerHour;
    }

    public int getRateLimitAuthenticatedPerHour() {
        return rateLimitAuthenticatedPerHour;
    }

    public String getAgentInvocationUrl() {
        return agentInvocationUrl;
    }

    public String getEmergencyBannerText() {
        return emergencyBannerText;
    }

    public boolean isProvideCitations() {
        return provideCitations;
    }
}
