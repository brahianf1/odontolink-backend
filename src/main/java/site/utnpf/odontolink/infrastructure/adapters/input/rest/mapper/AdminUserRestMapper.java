package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AdminUserDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper estático para convertir un {@link User} del dominio a su DTO de
 * presentación en el panel administrativo (RF05).
 *
 * El mapper omite deliberadamente el campo {@code password} para que ni
 * siquiera por accidente termine viajando en una respuesta HTTP.
 */
public final class AdminUserRestMapper {

    private AdminUserRestMapper() {
        // Clase de utilidades: no se instancia.
    }

    public static AdminUserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setActive(user.isActive());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDni(user.getDni());
        dto.setPhone(user.getPhone());
        dto.setBirthDate(user.getBirthDate());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public static List<AdminUserDTO> toDTOList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(AdminUserRestMapper::toDTO)
                .collect(Collectors.toList());
    }
}
