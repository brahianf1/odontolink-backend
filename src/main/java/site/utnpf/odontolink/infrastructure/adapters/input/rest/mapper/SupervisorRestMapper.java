package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.SupervisorDTO;

/**
 * Mapper para convertir entre Supervisor (dominio) y SupervisorDTO (REST).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class SupervisorRestMapper {

    /**
     * Convierte de modelo de dominio a DTO de respuesta.
     */
    public static SupervisorDTO toDTO(Supervisor supervisor) {
        if (supervisor == null) {
            return null;
        }

        SupervisorDTO dto = new SupervisorDTO();
        dto.setId(supervisor.getId());
        dto.setEmail(supervisor.getUser().getEmail());
        dto.setFirstName(supervisor.getUser().getFirstName());
        dto.setLastName(supervisor.getUser().getLastName());
        dto.setDni(supervisor.getUser().getDni());
        dto.setPhone(supervisor.getUser().getPhone());
        dto.setBirthDate(supervisor.getUser().getBirthDate());
        dto.setSpecialty(supervisor.getSpecialty());
        dto.setEmployeeId(supervisor.getEmployeeId());
        dto.setActive(supervisor.getUser().isActive());

        return dto;
    }
}
