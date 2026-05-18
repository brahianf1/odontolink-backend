package site.utnpf.odontolink.application.port.out;

/**
 * Puerto de salida que provee al filter de rate limiting las capacidades
 * vigentes del chatbot (RF29).
 *
 * <p>Diseno deliberado: el filter de seguridad NO depende del use case
 * completo del chatbot ni del repositorio de configuracion. Se inyecta este
 * puerto chico para que el filter solo conozca lo que necesita: los caps
 * configurables por admin. La implementacion en el adapter hace caching local
 * (TTL ~60s) para no martillar la BD por request.
 */
public interface IChatbotRateLimitPolicyPort {

    /** Snapshot de los caps vigentes. */
    record ChatbotRateLimits(int anonymousPerHour, int authenticatedPerHour) {
    }

    /**
     * Devuelve las capacidades vigentes desde {@code AiAgentConfiguration}.
     * Si la configuracion no existe (UNCONFIGURED) devuelve caps conservadores
     * por defecto para que el filter no rompa: {@code (1, 1)} bloqueara
     * cualquier abuso mientras el admin no haya cargado la config.
     */
    ChatbotRateLimits getCurrentLimits();
}
