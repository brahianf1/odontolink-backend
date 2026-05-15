package site.utnpf.odontolink.infrastructure.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.Collection;

/**
 * Implementacion propia de {@code UserDetails} que extiende la de Spring para
 * cargar dos campos extra que el {@link JwtAuthenticationFilter} necesita en
 * cada request:
 * <ul>
 *   <li>{@code userId}: clave primaria del usuario en nuestro dominio, util
 *       para no tener que volver a consultarla cuando ya hicimos el lookup
 *       inicial por email.</li>
 *   <li>{@code passwordChangedAt}: marca temporal contra la que se compara
 *       el claim {@code iat} del JWT para invalidar tokens emitidos antes
 *       de un cambio de credencial o desactivacion.</li>
 * </ul>
 *
 * Reutilizamos la subclase para evitar reimplementar boilerplate de
 * {@code UserDetails} (authorities, enabled, accountNonExpired, etc.).
 */
public class OdontolinkUserDetails extends User {

    private final Long userId;
    private final Instant passwordChangedAt;

    public OdontolinkUserDetails(Long userId,
                                 String username,
                                 String password,
                                 boolean enabled,
                                 boolean accountNonExpired,
                                 boolean credentialsNonExpired,
                                 boolean accountNonLocked,
                                 Collection<? extends GrantedAuthority> authorities,
                                 Instant passwordChangedAt) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired,
              accountNonLocked, authorities);
        this.userId = userId;
        this.passwordChangedAt = passwordChangedAt;
    }

    public Long getUserId() {
        return userId;
    }

    /**
     * @return marca temporal de la ultima rotacion/invalidacion. Puede ser
     *         {@code null} para cuentas creadas antes de introducir la
     *         columna; en ese caso el filtro JWT no aplica la comparacion.
     */
    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }
}
