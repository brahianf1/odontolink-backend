package site.utnpf.odontolink.application.port.out;

import site.utnpf.odontolink.domain.model.User;

/**
 * Puerto de salida (Output Port) para la generación de tokens de autenticación.
 * Permite que la capa de aplicación genere tokens sin depender de detalles de implementación (JWT).
 * Siguiendo el principio de inversión de dependencias de Clean Architecture.
 */
public interface ITokenProvider {

    /**
     * Genera un token de autenticación para el usuario especificado.
     *
     * @param user Usuario para el cual generar el token
     * @return Token de autenticación generado
     */
    String generateToken(User user);

    /**
     * Extrae el email del usuario desde un token.
     *
     * @param token Token de autenticación
     * @return Email del usuario
     */
    String getUserEmailFromToken(String token);

    /**
     * Valida si un token es válido.
     *
     * @param token Token a validar
     * @return true si el token es válido, false en caso contrario
     */
    boolean validateToken(String token);
}
