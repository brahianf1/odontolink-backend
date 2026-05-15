package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import org.openapitools.jackson.nullable.JsonNullable;
import site.utnpf.odontolink.application.port.in.UpdateProfileCommand;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateMyProfileRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MyProfileDTO;

import java.time.LocalDate;
import java.util.function.Function;

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
     * inmutable que la capa de aplicación entiende.
     *
     * Para los opcionales se preserva la semántica PATCH:
     * <ul>
     *   <li>{@code undefined()} en el DTO → {@code undefined()} en el
     *       command (no tocar).</li>
     *   <li>{@code present} con string vacío o sólo blancos → {@code present(null)}
     *       (limpiar el campo).</li>
     *   <li>{@code present} con valor → {@code present(trimmed)}.</li>
     * </ul>
     */
    public static UpdateProfileCommand toCommand(UpdateMyProfileRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return new UpdateProfileCommand(
                normalizeRequired(dto.getEmail()),
                normalizeRequired(dto.getFirstName()),
                normalizeRequired(dto.getLastName()),
                normalizeOptionalString(dto.getPhone()),
                mapOptional(dto.getBirthDate(), Function.identity()),
                normalizeOptionalString(dto.getAddress()),
                normalizeOptionalString(dto.getProfilePictureUrl())
        );
    }

    /**
     * Trim defensivo para campos requeridos. {@code @NotBlank} ya garantiza
     * que no llegan vacíos, pero un trim previo evita guardar valores con
     * espacios accidentales.
     */
    private static String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    /**
     * Aplica la semántica "vacío = limpiar" para opcionales tipo String.
     * Si el wrapper está undefined, devuelve undefined (no tocar). Si está
     * presente, hace trim y convierte cadenas vacías o nulas en
     * {@code present(null)} para indicar limpieza.
     */
    private static JsonNullable<String> normalizeOptionalString(JsonNullable<String> value) {
        if (value == null || !value.isPresent()) {
            return JsonNullable.undefined();
        }
        String raw = value.get();
        if (raw == null) {
            return JsonNullable.of(null);
        }
        String trimmed = raw.trim();
        return JsonNullable.of(trimmed.isEmpty() ? null : trimmed);
    }

    /**
     * Transforma un {@code JsonNullable<T>} preservando los tres estados:
     * undefined queda undefined, present con null queda present con null,
     * present con valor pasa por el mapeo.
     */
    private static <T, R> JsonNullable<R> mapOptional(JsonNullable<T> value, Function<T, R> mapper) {
        if (value == null || !value.isPresent()) {
            return JsonNullable.undefined();
        }
        T inner = value.get();
        return JsonNullable.of(inner == null ? null : mapper.apply(inner));
    }
}
