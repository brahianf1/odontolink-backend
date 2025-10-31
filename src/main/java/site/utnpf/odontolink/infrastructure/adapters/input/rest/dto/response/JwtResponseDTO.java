package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

/**
 * DTO de respuesta para operaciones de autenticación.
 * Contiene el token JWT y la información básica del usuario.
 */
public class JwtResponseDTO {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String role;
    private String firstName;
    private String lastName;

    // Constructores
    public JwtResponseDTO() {
    }

    public JwtResponseDTO(String token, Long userId, String email, String role, String firstName, String lastName) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
