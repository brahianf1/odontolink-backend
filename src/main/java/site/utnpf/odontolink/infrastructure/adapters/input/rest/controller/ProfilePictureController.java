package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.utnpf.odontolink.application.port.in.IProfilePictureUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ProfilePictureResponseDTO;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.io.IOException;

/**
 * Controlador REST para la gestion de foto de perfil del usuario autenticado.
 *
 * <p>Se mantiene en un controlador propio (no dentro de {@code ProfileController})
 * porque el contrato es distinto:
 * <ul>
 *   <li>Consume {@code multipart/form-data} en lugar de JSON.</li>
 *   <li>Depende del puerto de object storage que el resto del autoservicio
 *       de perfil no necesita.</li>
 * </ul>
 *
 * <p>El ID del usuario sobre el que opera la accion se obtiene del JWT via
 * {@link AuthenticationFacade}, garantizando que no haya IDOR.
 */
@RestController
@RequestMapping("/api/users/me/profile-picture")
@Tag(name = "Foto de perfil",
        description = "Endpoints multipart para subir o eliminar la foto de perfil del usuario autenticado")
@SecurityRequirement(name = "Bearer Authentication")
public class ProfilePictureController {

    private final IProfilePictureUseCase profilePictureUseCase;
    private final AuthenticationFacade authenticationFacade;

    public ProfilePictureController(IProfilePictureUseCase profilePictureUseCase,
                                    AuthenticationFacade authenticationFacade) {
        this.profilePictureUseCase = profilePictureUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    @Operation(
            summary = "Subir/sustituir foto de perfil",
            description = "Acepta un archivo de imagen JPEG o PNG en el campo 'file' del formulario. "
                    + "El backend valida el tipo real (no la extension), aplica crop centrado al cuadrado "
                    + "mas grande inscrito, redimensiona al tamanio configurado y reencoda como JPEG. "
                    + "Si el usuario tenia una foto previa propia, se elimina del storage al terminar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen procesada y URL publica devuelta",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProfilePictureResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Archivo invalido (formato no permitido, vacio o corrupto)"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "413", description = "Archivo supera el tamanio maximo permitido"),
            @ApiResponse(responseCode = "422", description = "El archivo supera la cuota especifica de fotos de perfil")
    })
    @RequestBody(
            description = "Multipart con un unico campo 'file' que contiene la imagen.",
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = MultipartFile.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfilePictureResponseDTO> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidBusinessRuleException("El archivo es obligatorio en el campo 'file'.");
        }

        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidBusinessRuleException(
                    "No se pudo leer el archivo subido: " + ex.getMessage());
        }
        String url = profilePictureUseCase.uploadProfilePicture(userId, bytes, file.getOriginalFilename());
        return ResponseEntity.ok(new ProfilePictureResponseDTO(url));
    }

    @Operation(
            summary = "Eliminar foto de perfil",
            description = "Borra la foto de perfil del usuario autenticado tanto del storage propio "
                    + "como del campo {@code profilePictureUrl} del perfil. Idempotente: si el usuario "
                    + "no tenia foto, la respuesta sigue siendo 204."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Foto eliminada (o ya no existia)"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido")
    })
    @DeleteMapping
    public ResponseEntity<Void> delete() {
        Long userId = authenticationFacade.getAuthenticatedUser().getId();
        profilePictureUseCase.deleteProfilePicture(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
