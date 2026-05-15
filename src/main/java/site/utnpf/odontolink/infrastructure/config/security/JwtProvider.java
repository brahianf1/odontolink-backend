package site.utnpf.odontolink.infrastructure.config.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Proveedor JWT para generar y validar tokens.
 * Utiliza la librería JJWT (io.jsonwebtoken).
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * SecretKey calculada una sola vez al arranque. Reconstruirla por cada
     * generacion/validacion de token implica re-derivar la clave HMAC en cada
     * request, lo cual es perfectamente innecesario porque el secreto es
     * inmutable durante el ciclo de vida de la aplicacion.
     */
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT a partir de la autenticación.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromEmail(userDetails.getUsername());
    }

    /**
     * Genera un token JWT a partir del email (username).
     */
    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extrae el email (subject) del token JWT.
     */
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Extrae la marca temporal {@code iat} (issued-at) del token. El filtro
     * JWT la usa para detectar tokens emitidos antes de un cambio de
     * contrasenia o de una invalidacion explicita de sesiones.
     */
    public java.time.Instant getIssuedAtFromToken(String token) {
        java.util.Date issuedAt = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();
        return issuedAt != null ? issuedAt.toInstant() : null;
    }

    /**
     * Valida el token JWT. Devuelve true si la firma y el formato son validos
     * y el token aun no ha expirado.
     *
     * Las fallas se registran a traves del logger SLF4J usando niveles que
     * reflejan la naturaleza del evento de seguridad: DEBUG para escenarios
     * frecuentes y benignos (token expirado), WARN para situaciones que
     * pueden indicar un cliente roto o un intento de manipulacion.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token JWT expirado para subject={}", ex.getClaims().getSubject());
        } catch (MalformedJwtException ex) {
            log.warn("Token JWT malformado: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.warn("Firma JWT invalida: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Token JWT no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Cadena de claims JWT vacia o nula");
        }
        return false;
    }
}
