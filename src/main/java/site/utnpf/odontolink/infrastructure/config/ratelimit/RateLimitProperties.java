package site.utnpf.odontolink.infrastructure.config.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propiedades de rate limiting agrupadas por politica.
 *
 * <p>Cada politica nombrada controla un bucket independiente con su propia
 * capacidad y periodo de refill. Mantener los limites en
 * {@code application.properties} (no hardcodeados) permite ajustarlos por
 * ambiente: limites mas laxos en dev, mas estrictos en prod.
 *
 * <p>Estructura YAML/properties equivalente:
 * <pre>
 * ratelimit:
 *   forgot-password-ip:
 *     capacity: 5
 *     period: PT1H
 *   forgot-password-email:
 *     capacity: 3
 *     period: PT1H
 *   reset-password-ip:
 *     capacity: 10
 *     period: PT1H
 *   login-ip:
 *     capacity: 10
 *     period: PT1M
 *   change-password-user:
 *     capacity: 5
 *     period: PT1H
 * </pre>
 */
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private Policy forgotPasswordIp = new Policy(5, Duration.ofHours(1));
    private Policy forgotPasswordEmail = new Policy(3, Duration.ofHours(1));
    private Policy resetPasswordIp = new Policy(10, Duration.ofHours(1));
    private Policy loginIp = new Policy(10, Duration.ofMinutes(1));
    private Policy changePasswordUser = new Policy(5, Duration.ofHours(1));

    public Policy getForgotPasswordIp() { return forgotPasswordIp; }
    public void setForgotPasswordIp(Policy v) { this.forgotPasswordIp = v; }

    public Policy getForgotPasswordEmail() { return forgotPasswordEmail; }
    public void setForgotPasswordEmail(Policy v) { this.forgotPasswordEmail = v; }

    public Policy getResetPasswordIp() { return resetPasswordIp; }
    public void setResetPasswordIp(Policy v) { this.resetPasswordIp = v; }

    public Policy getLoginIp() { return loginIp; }
    public void setLoginIp(Policy v) { this.loginIp = v; }

    public Policy getChangePasswordUser() { return changePasswordUser; }
    public void setChangePasswordUser(Policy v) { this.changePasswordUser = v; }

    public static class Policy {
        /** Maximo de tokens disponibles (capacidad del bucket). */
        private long capacity;
        /** Duracion completa de refill: en {@code period} se recupera la capacidad total. */
        private Duration period;

        public Policy() {}

        public Policy(long capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
        }

        public long getCapacity() { return capacity; }
        public void setCapacity(long v) { this.capacity = v; }

        public Duration getPeriod() { return period; }
        public void setPeriod(Duration v) { this.period = v; }
    }
}
