package site.utnpf.odontolink.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.domain.exception.AuthenticationFailedException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AuthResult;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

/**
 * Servicio de aplicación para autenticación (CU-001).
 * Implementa el puerto de entrada IAuthUseCase.
 * Coordina la autenticación del usuario y la generación del token.
 * El bean se registra explícitamente en BeanConfiguration.
 */
public class AuthService implements IAuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final ITokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                      ITokenProvider tokenProvider,
                      UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public AuthResult login(String email, String password) {
        try {
            // Autenticar las credenciales usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // Obtener el usuario del repositorio
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));

            // Generar el token usando el puerto (desacoplado de JWT)
            String token = tokenProvider.generateToken(user);

            // Retornar objeto de dominio tipado
            return new AuthResult(token, user);

        } catch (BadCredentialsException e) {
            throw new AuthenticationFailedException("Email o contraseña incorrectos", e);
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException("Error en la autenticación", e);
        }
    }
}
