package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IProfileUseCase;
import site.utnpf.odontolink.application.port.in.UpdateProfileCommand;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ChangeMyPasswordRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateMyProfileRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.MyProfileDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.MyProfileRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.Map;

/**
 * Controlador REST del autoservicio de perfil (RF06).
 *
 * Diseño de los endpoints:
 * <ul>
 *   <li>{@code GET /api/users/me}: lectura del propio perfil.</li>
 *   <li>{@code PUT /api/users/me}: actualización de los datos personales y de
 *       contacto (correo, teléfono, dirección, foto, nombre, fecha de
 *       nacimiento).</li>
 *   <li>{@code PUT /api/users/me/password}: rotación de la contraseña con
 *       verificación de la actual.</li>
 * </ul>
 *
 * Decisión de seguridad crítica: ninguno de los endpoints recibe un
 * identificador en la URL. El ID del usuario sobre el que opera la acción se
 * obtiene exclusivamente de {@link AuthenticationFacade}, que lo deriva del
 * JWT. Esto previene IDOR por construcción: aun si un atacante manipula el
 * cuerpo, no puede apuntar a otro usuario porque no hay forma de
 * referenciarlo.
 *
 * No se aplica {@link org.springframework.security.access.prepost.PreAuthorize}
 * por rol porque el RF06 explícitamente dice "todos los usuarios". La regla
 * declarativa {@code anyRequest().authenticated()} de {@code SecurityConfig}
 * basta para garantizar autenticación.
 */
@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Perfil del usuario",
        description = "Autoservicio del usuario autenticado para consultar y actualizar su propio perfil (RF06)")
@SecurityRequirement(name = "Bearer Authentication")
public class ProfileController {

    private final IProfileUseCase profileUseCase;
    private final AuthenticationFacade authenticationFacade;

    public ProfileController(IProfileUseCase profileUseCase,
                             AuthenticationFacade authenticationFacade) {
        this.profileUseCase = profileUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    @Operation(
            summary = "Obtener mi perfil",
            description = "Devuelve los datos del usuario autenticado. El identificador se obtiene del JWT, "
                    + "nunca de un parámetro de URL."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyProfileDTO.class))),
            @ApiResponse(responseCode = "401", description = "Solicitud sin token o con token inválido")
    })
    @GetMapping
    public ResponseEntity<MyProfileDTO> getMyProfile() {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        User user = profileUseCase.getMyProfile(userId);
        return ResponseEntity.ok(MyProfileRestMapper.toDTO(user));
    }

    @Operation(
            summary = "Actualizar mi perfil",
            description = "Permite al usuario autenticado modificar su información personal y de contacto. "
                    + "El email se valida contra la unicidad global. El DNI y el rol son inmutables desde "
                    + "este endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MyProfileDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos según las reglas de validación"),
            @ApiResponse(responseCode = "401", description = "Solicitud sin token o con token inválido"),
            @ApiResponse(responseCode = "409", description = "El email ya pertenece a otra cuenta")
    })
    @PutMapping
    public ResponseEntity<MyProfileDTO> updateMyProfile(@Valid @RequestBody UpdateMyProfileRequestDTO request) {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        UpdateProfileCommand command = MyProfileRestMapper.toCommand(request);
        User updated = profileUseCase.updateMyProfile(userId, command);
        return ResponseEntity.ok(MyProfileRestMapper.toDTO(updated));
    }

    @Operation(
            summary = "Cambiar mi contraseña",
            description = "Rota la contraseña del usuario autenticado. Requiere proporcionar la contraseña "
                    + "actual como prueba de identidad para mitigar abusos en caso de robo de sesión."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Contraseña actualizada correctamente."
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o nueva contraseña igual a la actual"),
            @ApiResponse(responseCode = "401", description = "Contraseña actual incorrecta o token inválido")
    })
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changeMyPassword(@Valid @RequestBody ChangeMyPasswordRequestDTO request) {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        profileUseCase.changeMyPassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }
}
