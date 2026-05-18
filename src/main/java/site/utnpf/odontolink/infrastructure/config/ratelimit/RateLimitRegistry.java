package site.utnpf.odontolink.infrastructure.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.exception.RateLimitExceededException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registro central de buckets de rate limiting.
 *
 * <p>Cada politica nombrada (definida en {@link RateLimitProperties}) tiene
 * un {@link ConcurrentMap} propio que mapea {@code key} (IP, email, userId)
 * a un {@link Bucket}. La asociacion es lazy: se crea el bucket en la primera
 * consulta para la clave.
 *
 * <p>Garbage collection: los buckets se mantienen indefinidamente en memoria
 * mientras la app vive. Para un VPS con uso normal esto es ruido despreciable
 * (un bucket ocupa ~200 bytes). Si en el futuro se vuelve significativo,
 * sustituir el {@link ConcurrentHashMap} por una cache con TTL (p.ej. Caffeine).
 */
@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitRegistry {

    public static final String FORGOT_PASSWORD_IP = "forgot-password-ip";
    public static final String FORGOT_PASSWORD_EMAIL = "forgot-password-email";
    public static final String RESET_PASSWORD_IP = "reset-password-ip";
    public static final String LOGIN_IP = "login-ip";
    public static final String CHANGE_PASSWORD_USER = "change-password-user";
    /** Buckets dinamicos del chatbot (capacidad la fija el admin desde panel). */
    public static final String CHATBOT_ANONYMOUS_IP = "chatbot-anon-ip";
    public static final String CHATBOT_AUTHENTICATED_USER = "chatbot-auth-user";

    private final RateLimitProperties props;
    private final ConcurrentMap<String, ConcurrentMap<String, Bucket>> bucketsByPolicy = new ConcurrentHashMap<>();

    public RateLimitRegistry(RateLimitProperties props) {
        this.props = props;
    }

    /**
     * Devuelve el bucket asociado a {@code (policy, key)}, creandolo si no
     * existe. Es seguro llamarlo concurrentemente para la misma clave.
     */
    public Bucket resolve(String policyName, String key) {
        ConcurrentMap<String, Bucket> buckets = bucketsByPolicy.computeIfAbsent(
                policyName, k -> new ConcurrentHashMap<>());
        return buckets.computeIfAbsent(key, k -> buildBucket(policyName));
    }

    /**
     * Intenta consumir 1 token; si no hay tokens disponibles lanza
     * {@link RateLimitExceededException} con el {@code retry-after} calculado
     * a partir del tiempo nanos hasta el proximo refill.
     */
    public void consumeOrThrow(String policyName, String key, String userFacingMessage) {
        Bucket bucket = resolve(policyName, key);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            throw new RateLimitExceededException(userFacingMessage, retryAfterSeconds);
        }
    }

    /**
     * Variante "silenciosa": consume si puede, devuelve {@code true} cuando
     * se aceptaron; {@code false} cuando el bucket estaba vacio. El caller
     * decide que hacer con la respuesta — util para flujos como forgot-password
     * donde el rate-limit debe ser observacionalmente indistinguible del
     * exito por anti-enumeracion.
     */
    public boolean tryConsume(String policyName, String key) {
        return resolve(policyName, key).tryConsume(1);
    }

    /**
     * Variante dinamica para politicas cuya capacidad la define el admin en
     * runtime (chatbot). La capacidad por hora se evalua solo al construir el
     * bucket por primera vez para esta {@code key}; cambios posteriores del
     * admin no rebuild el bucket existente (caro y propenso a inconsistencia).
     * En la practica los cambios propagan cuando el bucket es purgado por
     * restart o cuando la clave es nueva (otra IP, otro usuario).
     */
    public Bucket resolveDynamic(String policyName, String key, long capacityPerHour) {
        ConcurrentMap<String, Bucket> buckets = bucketsByPolicy.computeIfAbsent(
                policyName, k -> new ConcurrentHashMap<>());
        return buckets.computeIfAbsent(key, k -> buildDynamicBucket(capacityPerHour));
    }

    private Bucket buildBucket(String policyName) {
        RateLimitProperties.Policy policy = lookupPolicy(policyName);
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.getCapacity())
                .refillIntervally(policy.getCapacity(), policy.getPeriod())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket buildDynamicBucket(long capacityPerHour) {
        long capacity = Math.max(1L, capacityPerHour);
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private RateLimitProperties.Policy lookupPolicy(String name) {
        return switch (name) {
            case FORGOT_PASSWORD_IP -> props.getForgotPasswordIp();
            case FORGOT_PASSWORD_EMAIL -> props.getForgotPasswordEmail();
            case RESET_PASSWORD_IP -> props.getResetPasswordIp();
            case LOGIN_IP -> props.getLoginIp();
            case CHANGE_PASSWORD_USER -> props.getChangePasswordUser();
            default -> throw new IllegalArgumentException("Politica de rate-limit desconocida: " + name);
        };
    }
}
