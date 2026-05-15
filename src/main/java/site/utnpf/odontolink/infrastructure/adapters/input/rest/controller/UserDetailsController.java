package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IUserDetailsUseCase;
import site.utnpf.odontolink.application.port.in.MyDetailsView;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdatePatientDetailsRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateSupervisorDetailsRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MyDetailsDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.MyDetailsRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

/**
 * Controlador REST para los datos rol-especificos del usuario autenticado
 * (RF06 extension).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/users/me/details}: devuelve la union DTO con los
 *       campos aplicables al rol del usuario.</li>
 *   <li>{@code PATCH /api/users/me/details/patient}: solo PATIENT puede
 *       editar su {@code healthInsurance} y {@code bloodType}.</li>
 *   <li>{@code PATCH /api/users/me/details/supervisor}: solo SUPERVISOR
 *       puede editar su {@code specialty}.</li>
 * </ul>
 *
 * <p>Los campos identitarios ({@code studentId}, {@code studyYear},
 * {@code employeeId}) son inmutables desde autoservicio: cualquier cambio
 * sobre ellos debe pasar por el flujo administrativo correspondiente.
 *
 * <p>{@code @PreAuthorize} declarativo encarna la regla "este endpoint solo
 * lo invoca un usuario con tal rol" y deja al {@code GlobalExceptionHandler}
 * traducir el {@code AccessDeniedException} a HTTP 403. Mantenerlo aqui
 * complementa la validacion adicional en el service (que tambien chequea
 * el rol contra el repositorio).
 */
@RestController
@RequestMapping("/api/users/me/details")
@Tag(name = "Detalles rol-especificos del usuario",
        description = "Lectura y actualizacion de los campos especificos por rol (RF06 extension)")
@SecurityRequirement(name = "Bearer Authentication")
public class UserDetailsController {

    private final IUserDetailsUseCase userDetailsUseCase;
    private final AuthenticationFacade authenticationFacade;

    public UserDetailsController(IUserDetailsUseCase userDetailsUseCase,
                                 AuthenticationFacade authenticationFacade) {
        this.userDetailsUseCase = userDetailsUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    @Operation(
            summary = "Obtener mis datos rol-especificos",
            description = "Devuelve un DTO union con los campos aplicables al rol del usuario. "
                    + "Los campos no aplicables se omiten en el JSON (NON_NULL)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalles obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyDetailsDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido")
    })
    @GetMapping
    public ResponseEntity<MyDetailsDTO> getMyDetails() {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        MyDetailsView view = userDetailsUseCase.getMyDetails(userId);
        return ResponseEntity.ok(MyDetailsRestMapper.toDTO(view));
    }

    @Operation(
            summary = "Actualizar mis datos clinicos (PATIENT)",
            description = "Solo accesible para usuarios con rol PATIENT. Permite editar obra social y "
                    + "grupo sanguineo siguiendo semantica PATCH (campo ausente = no tocar; vacio = limpiar)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Datos actualizados"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol PATIENT")
    })
    @PreAuthorize("hasRole('PATIENT')")
    @PatchMapping("/patient")
    public ResponseEntity<Void> updatePatientDetails(
            @Valid @RequestBody UpdatePatientDetailsRequestDTO request) {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        userDetailsUseCase.updatePatientDetails(
                userId,
                request.getHealthInsurance(),
                request.getBloodType()
        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Actualizar mis datos academicos (SUPERVISOR)",
            description = "Solo accesible para usuarios con rol SUPERVISOR. Permite editar la "
                    + "especialidad. El legajo docente (employeeId) NO es editable desde aqui."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Datos actualizados"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol SUPERVISOR")
    })
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/supervisor")
    public ResponseEntity<Void> updateSupervisorDetails(
            @Valid @RequestBody UpdateSupervisorDetailsRequestDTO request) {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        userDetailsUseCase.updateSupervisorDetails(userId, request.getSpecialty());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
