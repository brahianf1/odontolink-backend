package site.utnpf.odontolink.domain.model;

/**
 * Objeto de dominio que representa el resultado de una autenticación exitosa.
 * Encapsula el token y la información del usuario autenticado.
 * Parte del dominio, no depende de detalles de infraestructura.
 */
public class AuthResult {

    private final String token;
    private final User user;

    public AuthResult(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }
}
