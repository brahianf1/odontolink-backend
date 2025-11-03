package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PractitionerDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.UserBasicDTO;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Practitioner (dominio) y PractitionerDTO (REST).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class PractitionerRestMapper {

    /**
     * Convierte de modelo de dominio a DTO de respuesta.
     */
    public static PractitionerDTO toDTO(Practitioner practitioner) {
        if (practitioner == null) {
            return null;
        }

        UserBasicDTO userDTO = new UserBasicDTO(
            practitioner.getUser().getId(),
            practitioner.getUser().getFirstName(),
            practitioner.getUser().getLastName(),
            practitioner.getUser().getDni(),
            practitioner.getUser().getEmail()
        );

        return new PractitionerDTO(
            practitioner.getId(),
            practitioner.getStudentId(),
            practitioner.getStudyYear(),
            userDTO
        );
    }

    /**
     * Convierte una lista de practicantes del dominio a DTOs.
     */
    public static List<PractitionerDTO> toDTOList(List<Practitioner> practitioners) {
        if (practitioners == null) {
            return List.of();
        }
        return practitioners.stream()
                .map(PractitionerRestMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte un conjunto de practicantes del dominio a DTOs.
     */
    public static Set<PractitionerDTO> toDTOSet(Set<Practitioner> practitioners) {
        if (practitioners == null) {
            return Set.of();
        }
        return practitioners.stream()
                .map(PractitionerRestMapper::toDTO)
                .collect(Collectors.toSet());
    }
}
