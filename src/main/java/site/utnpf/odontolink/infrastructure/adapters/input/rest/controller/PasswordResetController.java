package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IPasswordResetUseCase;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ForgotPasswordRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.ResetPasswordRequestDTO;

import java.util.Map;

/**
 * Controlador REST para el flujo de recuperación de contraseña (RF04).
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Se sirve bajo la ruta {@code /api/auth/**} para reutilizar la regla de
 * autorización ya declarada en SecurityConfig que permite el acceso público
 * a ese prefijo. Se mantiene en un controller separado del AuthController
 * para preservar el principio de responsabilidad única.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Recuperación de contraseña",
        description = "Endpoints públicos para solicitar y confirmar el restablecimiento de contraseña (RF04)")
public class PasswordResetController {

    private final IPasswordResetUseCase passwordResetUseCase;

    public PasswordResetController(IPasswordResetUseCase passwordResetUseCase) {
        this.passwordResetUseCase = passwordResetUseCase;
    }

    /**
     * POST /api/auth/forgot-password
     *
     * Solicita el envío del correo de recuperación. La respuesta es
     * uniformemente {@code 202 Accepted} con un cuerpo neutro: no se revela
     * si el email pertenece o no a una cuenta registrada, evitando enumeración.
     */
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Inicia el flujo de recuperación: si el email corresponde a una cuenta, se enviará "
                    + "un correo con un token de un solo uso. La respuesta es la misma exista o no la cuenta."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Solicitud aceptada (respuesta uniforme independientemente del email)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Si el email se encuentra registrado, se enviarán las instrucciones para restablecer la contraseña."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Email inválido o ausente")
    })
    @SecurityRequirement(name = "")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetUseCase.requestPasswordReset(request.getEmail());

        // Mensaje deliberadamente neutro: la lógica anti-enumeración debe ser
        // visible en la API también, no sólo en el servicio.
        Map<String, String> body = Map.of(
                "message",
                "Si el email se encuentra registrado, se enviarán las instrucciones para restablecer la contraseña."
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    /**
     * POST /api/auth/reset-password
     *
     * Consume un token de recuperación válido y actualiza la contraseña del
     * usuario asociado. El manejo de errores (token inválido/expirado/usado)
     * se delega al {@code GlobalExceptionHandler}.
     */
    @Operation(
            summary = "Confirmar restablecimiento de contraseña",
            description = "Recibe el token recibido por correo y la nueva contraseña. Si el token es válido, "
                    + "actualiza la credencial del usuario y consume el token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contraseña restablecida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Contraseña restablecida correctamente."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o token inválido, expirado o ya utilizado")
    })
    @SecurityRequirement(name = "")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetUseCase.confirmPasswordReset(request.getToken(), request.getNewPassword());

        Map<String, String> body = Map.of("message", "Contraseña restablecida correctamente.");
        return ResponseEntity.ok(body);
    }
}
