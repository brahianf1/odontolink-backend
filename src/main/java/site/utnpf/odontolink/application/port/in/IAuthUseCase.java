package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AuthResult;

/**
 * Puerto de entrada (Use Case) para autenticación.
 * Define el contrato que deben cumplir los servicios de aplicación.
 * Retorna objetos de dominio, no estructuras genéricas.
 */
public interface IAuthUseCase {

    /**
     * Autentica un usuario y genera un token de autenticación.
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @return AuthResult con el token y la información del usuario
     * @throws site.utnpf.odontolink.domain.exception.AuthenticationFailedException si las credenciales son inválidas
     */
    AuthResult login(String email, String password);
}
