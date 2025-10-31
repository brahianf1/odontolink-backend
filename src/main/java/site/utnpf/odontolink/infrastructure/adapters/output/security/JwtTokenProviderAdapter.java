package site.utnpf.odontolink.infrastructure.adapters.output.security;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.config.security.JwtProvider;

/**
 * Adaptador de salida (Output Adapter) que implementa ITokenProvider usando JWT.
 * Desacopla la capa de aplicación de los detalles de implementación de tokens.
 * Sigue el patrón Adapter de arquitectura hexagonal.
 */
@Component
public class JwtTokenProviderAdapter implements ITokenProvider {

    private final JwtProvider jwtProvider;

    public JwtTokenProviderAdapter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public String generateToken(User user) {
        return jwtProvider.generateTokenFromEmail(user.getEmail());
    }

    @Override
    public String getUserEmailFromToken(String token) {
        return jwtProvider.getEmailFromToken(token);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }
}
