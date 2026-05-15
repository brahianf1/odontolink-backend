package site.utnpf.odontolink.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Filtro JWT que intercepta cada request para validar el token.
 * Se ejecuta una vez por cada petición HTTP.
 *
 * <p>Capa extra de invalidación (Fase 2): compara el claim {@code iat} del
 * token contra {@link OdontolinkUserDetails#getPasswordChangedAt()}. Si el
 * token fue emitido antes de la última rotación de credenciales o de un
 * logout-all explícito, se descarta sin autenticar la request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                authenticate(request, jwt);
            }
        } catch (UsernameNotFoundException ex) {
            // Token criptograficamente valido pero el sujeto ya no existe o esta
            // inactivo: caso esperado (usuario eliminado, deshabilitado, token de
            // tenant antiguo). No es un fallo del sistema, asi que evitamos el
            // ruido de un stacktrace ERROR y dejamos que el EntryPoint conteste 401.
            SecurityContextHolder.clearContext();
            if (logger.isDebugEnabled()) {
                logger.debug("JWT presentado para un usuario inexistente o inactivo: " + ex.getMessage());
            }
        } catch (Exception ex) {
            // Cualquier otra falla (parsing inesperado, fallo del repositorio,
            // etc.) si merece nivel ERROR porque indica un problema real.
            SecurityContextHolder.clearContext();
            logger.error("No se pudo establecer la autenticacion del usuario en el contexto de seguridad", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String jwt) {
        String email = jwtProvider.getEmailFromToken(jwt);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!isJwtFreshAfterCredentialChange(jwt, userDetails)) {
            // Token emitido antes del ultimo cambio de credencial / logout-all
            // / desactivacion administrativa. No autenticamos; la cadena seguira
            // pero el endpoint protegido devolvera 401 al no haber Authentication.
            if (logger.isDebugEnabled()) {
                logger.debug("JWT descartado: emitido antes del passwordChangedAt del usuario "
                        + userDetails.getUsername());
            }
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Devuelve {@code true} cuando el token fue emitido en o despues del
     * ultimo evento que invalida sesiones del usuario. Si el usuario no es
     * {@link OdontolinkUserDetails} o el campo es {@code null} (cuentas
     * pre-migracion), aceptamos sin comparar.
     *
     * <p>Truncamos a segundos antes de comparar porque el claim {@code iat}
     * tiene precision de segundos (estandar JWT) y {@code Instant} en Java
     * tiene precision de nanos: comparar directamente produciria falsos
     * positivos cuando el bump y el {@code iat} caen dentro del mismo segundo.
     */
    private boolean isJwtFreshAfterCredentialChange(String jwt, UserDetails userDetails) {
        if (!(userDetails instanceof OdontolinkUserDetails details)) {
            return true;
        }
        Instant passwordChangedAt = details.getPasswordChangedAt();
        if (passwordChangedAt == null) {
            return true;
        }
        Instant issuedAt = jwtProvider.getIssuedAtFromToken(jwt);
        if (issuedAt == null) {
            // Un JWT sin iat es atipico pero deja la puerta abierta a tokens
            // legacy si los hubiera; preferimos rechazar.
            return false;
        }
        Instant issuedAtTruncated = issuedAt.truncatedTo(ChronoUnit.SECONDS);
        Instant changedAtTruncated = passwordChangedAt.truncatedTo(ChronoUnit.SECONDS);
        return !issuedAtTruncated.isBefore(changedAtTruncated);
    }

    /**
     * Extrae el JWT del header Authorization.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
