package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.UpdateProfileCommand;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateMyProfileRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MyProfileDTO;

/**
 * Mapper estático entre los DTOs REST del autoservicio de perfil (RF06) y los
 * tipos de la capa de aplicación/dominio.
 *
 * Se mantiene en {@code static} siguiendo el patrón del resto de mappers REST
 * del proyecto ({@code AdminUserRestMapper}, {@code AppointmentRestMapper},
 * etc.). El mapper omite deliberadamente el campo {@code password} en el DTO
 * de respuesta: nunca debe viajar al cliente, ni siquiera como cadena vacía.
 */
public final class MyProfileRestMapper {

    private MyProfileRestMapper() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Convierte el modelo de dominio {@link User} a la vista de autoservicio.
     */
    public static MyProfileDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        MyProfileDTO dto = new MyProfileDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setActive(user.isActive());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDni(user.getDni());
        dto.setPhone(user.getPhone());
        dto.setBirthDate(user.getBirthDate());
        dto.setAddress(user.getAddress());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    /**
     * Convierte el payload del cliente en un {@link UpdateProfileCommand}
     * inmutable que la capa de aplicación entiende. La normalización ligera
     * (trim del email) vive aquí para que el caso de uso no tenga que
     * preocuparse por inputs accidentalmente acolchados con espacios.
     */
    public static UpdateProfileCommand toCommand(UpdateMyProfileRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return new UpdateProfileCommand(
                normalize(dto.getEmail()),
                normalize(dto.getFirstName()),
                normalize(dto.getLastName()),
                normalize(dto.getPhone()),
                dto.getBirthDate(),
                normalize(dto.getAddress()),
                normalize(dto.getProfilePictureUrl())
        );
    }

    /**
     * Aplica un trim defensivo y convierte cadenas vacías en {@code null}.
     * Tratar "" como null permite que el usuario "limpie" un campo opcional
     * (teléfono, dirección, foto) enviando una cadena vacía desde el frontend.
     */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
