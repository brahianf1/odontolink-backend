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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.domain.model.AuthResult;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.LoginRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.RegisterPatientRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.RegisterPractitionerRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.JwtResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AuthResponseMapper;

import java.util.Map;

/**
 * Controlador REST para operaciones de autenticación.
 * Expone los endpoints públicos de registro y login.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints públicos para registro de usuarios y autenticación en el sistema")
public class AuthController {

    private final IPatientRegistrationUseCase patientRegistrationUseCase;
    private final IPractitionerRegistrationUseCase practitionerRegistrationUseCase;
    private final IAuthUseCase authUseCase;

    public AuthController(IPatientRegistrationUseCase patientRegistrationUseCase,
                         IPractitionerRegistrationUseCase practitionerRegistrationUseCase,
                         IAuthUseCase authUseCase) {
        this.patientRegistrationUseCase = patientRegistrationUseCase;
        this.practitionerRegistrationUseCase = practitionerRegistrationUseCase;
        this.authUseCase = authUseCase;
    }

    /**
     * POST /api/auth/register/patient
     * Registra un nuevo paciente en el sistema.
     * El manejo de excepciones se delega a GlobalExceptionHandler.
     * El mapeo a DTO se delega a AuthResponseMapper.
     */
    @Operation(
            summary = "Registrar nuevo paciente",
            description = "Crea una nueva cuenta de paciente en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Paciente registrado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Paciente registrado exitosamente",
                                              "userId": 15,
                                              "email": "carlos.rodriguez@gmail.com",
                                              "role": "PATIENT"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o email/DNI ya registrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del paciente a registrar",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "email": "carlos.rodriguez@gmail.com",
                                      "password": "MiPass123!",
                                      "firstName": "Carlos",
                                      "lastName": "Rodríguez",
                                      "dni": "35789456",
                                      "phone": "3815234567",
                                      "birthDate": "1995-06-15",
                                      "healthInsurance": "OSDE",
                                      "bloodType": "O+"
                                    }
                                    """
                    )
            )
    )
    @SecurityRequirement(name = "")
    @PostMapping("/register/patient")
    public ResponseEntity<Map<String, Object>> registerPatient(@Valid @RequestBody RegisterPatientRequestDTO request) {
        Patient patient = patientRegistrationUseCase.registerPatient(
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

        Map<String, Object> response = AuthResponseMapper.toRegistrationResponseDTO(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/register/practitioner
     * Registra un nuevo practicante en el sistema.
     * El manejo de excepciones se delega a GlobalExceptionHandler.
     * El mapeo a DTO se delega a AuthResponseMapper.
     */
    @Operation(
            summary = "Registrar nuevo practicante",
            description = "Crea una nueva cuenta de practicante (estudiante de odontología)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Practicante registrado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Practicante registrado exitosamente",
                                              "userId": 8,
                                              "email": "ana.martinez@fodo.unt.edu.ar",
                                              "role": "PRACTITIONER"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o email/DNI ya registrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del practicante a registrar",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "email": "ana.martinez@fodo.unt.edu.ar",
                                      "password": "Segura456!",
                                      "firstName": "Ana",
                                      "lastName": "Martínez",
                                      "dni": "38456123",
                                      "phone": "3816789012",
                                      "birthDate": "1998-03-20",
                                      "studentId": "48765",
                                      "studyYear": 4
                                    }
                                    """
                    )
            )
    )
    @SecurityRequirement(name = "")
    @PostMapping("/register/practitioner")
    public ResponseEntity<Map<String, Object>> registerPractitioner(@Valid @RequestBody RegisterPractitionerRequestDTO request) {
        Practitioner practitioner = practitionerRegistrationUseCase.registerPractitioner(
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

        Map<String, Object> response = AuthResponseMapper.toRegistrationResponseDTO(practitioner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Autentica un usuario y devuelve un token JWT.
     * El manejo de excepciones se delega a GlobalExceptionHandler.
     * El mapeo a DTO se delega a AuthResponseMapper.
     */
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario y devuelve un token JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxNSIsImVtYWlsIjoiY2FybG9zLnJvZHJpZ3VlekBnbWFpbC5jb20iLCJyb2xlIjoiUEFUSUVOVCJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                                              "type": "Bearer",
                                              "userId": 15,
                                              "email": "carlos.rodriguez@gmail.com",
                                              "role": "PATIENT",
                                              "firstName": "Carlos",
                                              "lastName": "Rodríguez"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas",
                    content = @Content(mediaType = "application/json")
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credenciales de acceso",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "email": "carlos.rodriguez@gmail.com",
                                      "password": "MiPass123!"
                                    }
                                    """
                    )
            )
    )
    @SecurityRequirement(name = "")
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResult authResult = authUseCase.login(
                request.getEmail(),
                request.getPassword()
        );

        JwtResponseDTO response = AuthResponseMapper.toJwtResponseDTO(authResult);
        return ResponseEntity.ok(response);
    }
}
