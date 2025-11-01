package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

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
