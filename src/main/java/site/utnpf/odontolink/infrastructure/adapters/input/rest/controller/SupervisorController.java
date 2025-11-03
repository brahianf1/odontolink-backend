package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.RegisterSupervisorRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.SupervisorDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.SupervisorRestMapper;

/**
 * Controlador REST para la gestión de supervisores/docentes.
 * Expone endpoints para el registro y gestión de supervisores.
 * Puerto de entrada (Input Adapter).
 */
@RestController
@RequestMapping("/api/supervisors")
@Tag(name = "Supervisores", description = "Operaciones para registro y gestión de supervisores/docentes del sistema")
public class SupervisorController {

    private final ISupervisorRegistrationUseCase supervisorRegistrationUseCase;

    public SupervisorController(ISupervisorRegistrationUseCase supervisorRegistrationUseCase) {
        this.supervisorRegistrationUseCase = supervisorRegistrationUseCase;
    }

    /**
     * Endpoint para registrar un nuevo supervisor/docente.
     *
     * @param request DTO con los datos del supervisor a registrar
     * @return SupervisorDTO con la información del supervisor registrado
     */
    @PostMapping("/register")
    public ResponseEntity<SupervisorDTO> registerSupervisor(@Valid @RequestBody RegisterSupervisorRequestDTO request) {
        Supervisor supervisor = supervisorRegistrationUseCase.registerSupervisor(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getDni(),
                request.getPhone(),
                request.getBirthDate(),
                request.getSpecialty(),
                request.getEmployeeId()
        );

        SupervisorDTO response = SupervisorRestMapper.toDTO(supervisor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
