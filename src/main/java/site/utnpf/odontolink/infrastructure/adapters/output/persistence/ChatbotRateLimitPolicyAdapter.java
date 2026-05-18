package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.out.IChatbotRateLimitPolicyPort;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Adaptador que provee al filter de rate limiting las capacidades vigentes
 * desde {@link AiAgentConfigurationRepository} con caching local (TTL 60s)
 * para no martillar la BD por cada request al chatbot.
 *
 * <p>Defaults conservadores si la configuracion no existe: {@code (1, 1)}
 * bloquearia al primer mensaje, asegurando que la app nunca opere "abierta"
 * por error de bootstrap. En la practica al desplegar el admin sube la
 * config al PUBLISH inicial y los caps reales toman valor.
 */
@Component
@Transactional(readOnly = true)
public class ChatbotRateLimitPolicyAdapter implements IChatbotRateLimitPolicyPort {

    private static final long CACHE_TTL_MS = 60_000L;

    private final AiAgentConfigurationRepository configRepository;
    private final AtomicReference<Cached> cache = new AtomicReference<>(
            new Cached(0L, new ChatbotRateLimits(1, 1)));

    public ChatbotRateLimitPolicyAdapter(AiAgentConfigurationRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public ChatbotRateLimits getCurrentLimits() {
        long now = System.currentTimeMillis();
        Cached snap = cache.get();
        if (now - snap.timestamp() < CACHE_TTL_MS) {
            return snap.value();
        }
        ChatbotRateLimits fresh = configRepository.findSingleton()
                .map(c -> new ChatbotRateLimits(
                        c.getRateLimitAnonymousPerHour(),
                        c.getRateLimitAuthenticatedPerHour()))
                .orElse(new ChatbotRateLimits(1, 1));
        cache.set(new Cached(now, fresh));
        return fresh;
    }

    /** Permite forzar refresh al editor admin si llega un cambio en el futuro. */
    public void invalidate() {
        cache.set(new Cached(0L, new ChatbotRateLimits(1, 1)));
    }

    private record Cached(long timestamp, ChatbotRateLimits value) {
    }
}
