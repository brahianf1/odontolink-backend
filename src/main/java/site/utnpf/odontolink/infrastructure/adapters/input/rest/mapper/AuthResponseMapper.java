package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.AuthResult;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.JwtResponseDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper para convertir objetos del dominio a DTOs de respuesta de autenticación.
 * Centraliza la lógica de mapeo, manteniendo los controladores limpios.
 * Sigue el patrón Mapper en arquitectura hexagonal.
 */
public class AuthResponseMapper {

    private AuthResponseMapper() {
        // Utility class
    }

    /**
     * Convierte un AuthResult del dominio a JwtResponseDTO de infraestructura.
     */
    public static JwtResponseDTO toJwtResponseDTO(AuthResult authResult) {
        User user = authResult.getUser();
        return new JwtResponseDTO(
                authResult.getToken(),
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    /**
     * Convierte un Patient del dominio a un DTO de respuesta de registro.
     */
    public static Map<String, Object> toRegistrationResponseDTO(Patient patient) {
        User user = patient.getUser();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Paciente registrado exitosamente");
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        return response;
    }

    /**
     * Convierte un Practitioner del dominio a un DTO de respuesta de registro.
     */
    public static Map<String, Object> toRegistrationResponseDTO(Practitioner practitioner) {
        User user = practitioner.getUser();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Practicante registrado exitosamente");
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        return response;
    }
}
