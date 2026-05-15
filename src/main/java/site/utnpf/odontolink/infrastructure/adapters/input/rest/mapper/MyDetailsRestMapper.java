package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.MyDetailsView;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MyDetailsDTO;

/**
 * Mapper estatico entre {@link MyDetailsView} (capa de aplicacion) y
 * {@link MyDetailsDTO} (REST).
 */
public final class MyDetailsRestMapper {

    private MyDetailsRestMapper() {
    }

    public static MyDetailsDTO toDTO(MyDetailsView view) {
        if (view == null) {
            return null;
        }
        MyDetailsDTO dto = new MyDetailsDTO();
        dto.setUserId(view.getUserId());
        dto.setRole(view.getRole());
        dto.setHealthInsurance(view.getHealthInsurance());
        dto.setBloodType(view.getBloodType());
        dto.setStudentId(view.getStudentId());
        dto.setStudyYear(view.getStudyYear());
        dto.setSpecialty(view.getSpecialty());
        dto.setEmployeeId(view.getEmployeeId());
        return dto;
    }
}
