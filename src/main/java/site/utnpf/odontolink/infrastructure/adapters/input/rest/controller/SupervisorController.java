package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorUseCase;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.BatchLinkPractitionersRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.RegisterSupervisorRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PractitionerDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.SupervisorDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.PractitionerRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.SupervisorRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;
import java.util.Set;

/**
 * Controlador REST para la gestión de supervisores/docentes.
 * Expone endpoints para:
 * - Registro de supervisores
 * - Vinculación académica de practicantes (RF22, RF37)
 * - Visualización y búsqueda de practicantes (RF35, RF38)
 *
 * Puerto de entrada (Input Adapter).
 */
@RestController
@RequestMapping("/api/supervisors")
@Tag(name = "Supervisores", description = "Operaciones para registro, gestión de supervisores/docentes y vinculación académica")
public class SupervisorController {

    private final ISupervisorRegistrationUseCase supervisorRegistrationUseCase;
    private final ISupervisorUseCase supervisorUseCase;
    private final AuthenticationFacade authenticationFacade;

    public SupervisorController(
            ISupervisorRegistrationUseCase supervisorRegistrationUseCase,
            ISupervisorUseCase supervisorUseCase,
            AuthenticationFacade authenticationFacade) {
        this.supervisorRegistrationUseCase = supervisorRegistrationUseCase;
        this.supervisorUseCase = supervisorUseCase;
        this.authenticationFacade = authenticationFacade;
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

    /**
     * Endpoint para vincular un practicante al supervisor autenticado.
     * Implementa CU 7.1: Vincular Practicante (RF22, RF37).
     *
     * @param practitionerId ID del practicante a vincular
     * @return ResponseEntity sin contenido (204 NO_CONTENT)
     */
    @PostMapping("/my-practitioners/{practitionerId}")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Vincular practicante",
        description = "Vincula un practicante a la lista de alumnos supervisados del supervisor autenticado. " +
                      "Requiere rol SUPERVISOR."
    )
    public ResponseEntity<Void> linkPractitioner(
            @Parameter(description = "ID del practicante a vincular", required = true)
            @PathVariable Long practitionerId) {
        User currentUser = authenticationFacade.getAuthenticatedUser();
        supervisorUseCase.linkPractitionerToSupervisor(practitionerId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para desvincular un practicante del supervisor autenticado.
     * Implementa CU 7.2: Desvincular Practicante.
     *
     * @param practitionerId ID del practicante a desvincular
     * @return ResponseEntity sin contenido (204 NO_CONTENT)
     */
    @DeleteMapping("/my-practitioners/{practitionerId}")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Desvincular practicante",
        description = "Desvincula un practicante de la lista de alumnos supervisados del supervisor autenticado. " +
                      "Requiere rol SUPERVISOR."
    )
    public ResponseEntity<Void> unlinkPractitioner(
            @Parameter(description = "ID del practicante a desvincular", required = true)
            @PathVariable Long practitionerId) {
        User currentUser = authenticationFacade.getAuthenticatedUser();
        supervisorUseCase.unlinkPractitionerFromSupervisor(practitionerId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para obtener la lista de practicantes supervisados.
     * Implementa CU 7.3 (Endpoint A): Obtener mis practicantes (RF35).
     *
     * @return Set de PractitionerDTO con los practicantes supervisados
     */
    @GetMapping("/my-practitioners")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Obtener practicantes a cargo",
        description = "Retorna la lista de practicantes que el supervisor autenticado tiene a su cargo. " +
                      "Requiere rol SUPERVISOR."
    )
    public ResponseEntity<Set<PractitionerDTO>> getMyPractitioners() {
        User currentUser = authenticationFacade.getAuthenticatedUser();
        Set<Practitioner> practitioners = supervisorUseCase.getMyPractitioners(currentUser);
        Set<PractitionerDTO> response = PractitionerRestMapper.toDTOSet(practitioners);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para vincular múltiples practicantes al supervisor autenticado (operación batch).
     * Permite vincular varios practicantes en una sola operación transaccional.
     *
     * @param request DTO con la lista de IDs de practicantes a vincular
     * @return ResponseEntity sin contenido (204 NO_CONTENT)
     */
    @PostMapping("/my-practitioners/batch")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Vincular múltiples practicantes",
        description = "Vincula múltiples practicantes a la lista de alumnos supervisados del supervisor autenticado. " +
                      "Operación batch que permite vincular varios practicantes de una vez. " +
                      "Los vínculos duplicados son ignorados (operación idempotente). " +
                      "Requiere rol SUPERVISOR."
    )
    public ResponseEntity<Void> linkMultiplePractitioners(
            @Parameter(description = "Lista de IDs de practicantes a vincular", required = true)
            @Valid @RequestBody BatchLinkPractitionersRequestDTO request) {
        User currentUser = authenticationFacade.getAuthenticatedUser();
        supervisorUseCase.linkMultiplePractitioners(request.getPractitionerIds(), currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para buscar o listar practicantes globalmente.
     * Implementa CU 7.3 (Endpoint B): Buscar/Listar practicantes (RF38).
     *
     * @param query Término de búsqueda opcional (nombre, DNI o legajo). Si no se proporciona, retorna todos
     * @return List de PractitionerDTO con los resultados de la búsqueda o todos los practicantes
     */
    @GetMapping("/practitioners/search")
    @PreAuthorize("hasRole('ROLE_SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Buscar o listar practicantes",
        description = "Busca practicantes en el sistema por nombre completo, DNI o legajo. " +
                      "Si no se proporciona el parámetro 'query', retorna todos los practicantes disponibles. " +
                      "Permite al supervisor explorar y encontrar practicantes para vincular. " +
                      "Requiere rol SUPERVISOR."
    )
    public ResponseEntity<List<PractitionerDTO>> searchPractitioners(
            @Parameter(
                description = "Término de búsqueda opcional (nombre, DNI o legajo). Si no se proporciona, retorna todos",
                required = false
            )
            @RequestParam(required = false) String query) {
        List<Practitioner> practitioners = supervisorUseCase.searchPractitioners(query);
        List<PractitionerDTO> response = PractitionerRestMapper.toDTOList(practitioners);
        return ResponseEntity.ok(response);
    }
}
