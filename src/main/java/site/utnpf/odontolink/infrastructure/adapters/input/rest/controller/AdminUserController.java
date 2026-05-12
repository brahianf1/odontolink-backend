package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IAdminUserManagementUseCase;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AdminCreatePatientRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AdminCreatePractitionerRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AdminCreateSupervisorRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateUserProfileRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AdminUserDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AdminUserRestMapper;

import java.util.List;

/**
 * Adaptador de entrada REST para la gestión administrativa de usuarios (RF05).
 *
 * Todos los endpoints quedan bajo {@code /api/admin/users} y se restringen
 * al rol {@code ROLE_ADMIN} mediante {@link PreAuthorize} a nivel de clase.
 * Se complementa con la regla declarativa en {@code SecurityConfig}, dando
 * defensa en profundidad: aun si alguien retira el {@code @PreAuthorize},
 * la cadena de filtros igualmente bloqueará el acceso.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administración - Usuarios",
        description = "Operaciones del administrador para listar, crear, modificar y dar de baja usuarios (RF05)")
public class AdminUserController {

    private final IAdminUserManagementUseCase adminUserManagementUseCase;

    public AdminUserController(IAdminUserManagementUseCase adminUserManagementUseCase) {
        this.adminUserManagementUseCase = adminUserManagementUseCase;
    }

    @Operation(
            summary = "Listar usuarios",
            description = "Devuelve los usuarios del sistema. Soporta filtros opcionales por rol, estado " +
                    "(activo/inactivo) y un término de búsqueda libre que matchea email, DNI o nombre completo."
    )
    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> listUsers(
            @Parameter(description = "Filtra por rol (ROLE_PATIENT, ROLE_PRACTITIONER, ROLE_SUPERVISOR, ROLE_ADMIN)")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Filtra por estado de activación")
            @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Búsqueda libre por email, DNI o nombre completo")
            @RequestParam(required = false) String query) {

        List<User> users = adminUserManagementUseCase.listUsers(role, isActive, query);
        return ResponseEntity.ok(AdminUserRestMapper.toDTOList(users));
    }

    @Operation(summary = "Obtener detalle de un usuario por ID")
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable Long id) {
        User user = adminUserManagementUseCase.getUserById(id);
        return ResponseEntity.ok(AdminUserRestMapper.toDTO(user));
    }

    @Operation(
            summary = "Crear paciente",
            description = "Crea un nuevo paciente desde el panel administrativo. Aplica las mismas " +
                    "validaciones de unicidad (email y DNI) que el auto-registro."
    )
    @PostMapping("/patient")
    public ResponseEntity<AdminUserDTO> createPatient(@Valid @RequestBody AdminCreatePatientRequestDTO request) {
        Patient patient = adminUserManagementUseCase.createPatient(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getDni(),
                request.getPhone(),
                request.getBirthDate(),
                request.getHealthInsurance(),
                request.getBloodType()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminUserRestMapper.toDTO(patient.getUser()));
    }

    @Operation(
            summary = "Crear practicante",
            description = "Crea un nuevo practicante/estudiante de odontología. Aplica las validaciones " +
                    "de unicidad de email, DNI y legajo del flujo de auto-registro."
    )
    @PostMapping("/practitioner")
    public ResponseEntity<AdminUserDTO> createPractitioner(@Valid @RequestBody AdminCreatePractitionerRequestDTO request) {
        Practitioner practitioner = adminUserManagementUseCase.createPractitioner(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getDni(),
                request.getPhone(),
                request.getBirthDate(),
                request.getStudentId(),
                request.getStudyYear()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminUserRestMapper.toDTO(practitioner.getUser()));
    }

    @Operation(
            summary = "Crear supervisor/docente",
            description = "Crea un nuevo supervisor/docente. Aplica las validaciones de unicidad de " +
                    "email, DNI y legajo docente del flujo de auto-registro."
    )
    @PostMapping("/supervisor")
    public ResponseEntity<AdminUserDTO> createSupervisor(@Valid @RequestBody AdminCreateSupervisorRequestDTO request) {
        Supervisor supervisor = adminUserManagementUseCase.createSupervisor(
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminUserRestMapper.toDTO(supervisor.getUser()));
    }

    @Operation(
            summary = "Modificar perfil de usuario",
            description = "Permite al administrador actualizar nombre, apellido, teléfono y fecha de " +
                    "nacimiento del usuario indicado. Email, DNI, rol y contraseña no son modificables " +
                    "desde este endpoint."
    )
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDTO> updateUserProfile(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateUserProfileRequestDTO request) {
        User updated = adminUserManagementUseCase.updateUserProfile(
                id,
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getBirthDate()
        );
        return ResponseEntity.ok(AdminUserRestMapper.toDTO(updated));
    }

    @Operation(
            summary = "Dar de baja un usuario (baja lógica)",
            description = "Marca al usuario como inactivo (isActive=false). El registro se conserva " +
                    "para preservar la trazabilidad clínica y administrativa."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<AdminUserDTO> deactivateUser(@PathVariable Long id) {
        User deactivated = adminUserManagementUseCase.deactivateUser(id);
        return ResponseEntity.ok(AdminUserRestMapper.toDTO(deactivated));
    }

    @Operation(
            summary = "Reactivar un usuario dado de baja",
            description = "Revierte una baja lógica previa. Disponible exclusivamente para administradores."
    )
    @PostMapping("/{id}/activate")
    public ResponseEntity<AdminUserDTO> reactivateUser(@PathVariable Long id) {
        User reactivated = adminUserManagementUseCase.reactivateUser(id);
        return ResponseEntity.ok(AdminUserRestMapper.toDTO(reactivated));
    }
}
