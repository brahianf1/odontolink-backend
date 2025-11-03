package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.ITreatmentUseCase;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CreateTreatmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.TreatmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.TreatmentRestMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión del catálogo maestro de tratamientos.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints:
 * - POST   /api/treatments       - Crear tratamiento (solo ADMIN)
 * - GET    /api/treatments       - Listar todos los tratamientos (autenticados)
 * - GET    /api/treatments/{id}  - Obtener tratamiento por ID (autenticados)
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/treatments")
@Tag(name = "Tratamientos", description = "Gestión del catálogo maestro de tratamientos odontológicos disponibles en el sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class TreatmentController {

    private final ITreatmentUseCase treatmentUseCase;

    public TreatmentController(ITreatmentUseCase treatmentUseCase) {
        this.treatmentUseCase = treatmentUseCase;
    }

    /**
     * Crea un nuevo tratamiento en el catálogo maestro.
     * Solo accesible por administradores.
     *
     * POST /api/treatments
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TreatmentResponseDTO> createTreatment(@Valid @RequestBody CreateTreatmentRequestDTO request) {

        Treatment treatment = treatmentUseCase.createTreatment(
                request.getName(),
                request.getDescription(),
                request.getArea()
        );

        TreatmentResponseDTO response = TreatmentRestMapper.toResponse(treatment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene todos los tratamientos del catálogo maestro.
     * Accesible por todos los usuarios autenticados.
     *
     * GET /api/treatments
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TreatmentResponseDTO>> getAllTreatments() {

        List<Treatment> treatments = treatmentUseCase.getAllTreatments();
        List<TreatmentResponseDTO> response = treatments.stream()
                .map(TreatmentRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene un tratamiento específico por su ID.
     * Accesible por todos los usuarios autenticados.
     *
     * GET /api/treatments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TreatmentResponseDTO> getTreatmentById(@PathVariable Long id) {

        Treatment treatment = treatmentUseCase.getTreatmentById(id);
        TreatmentResponseDTO response = TreatmentRestMapper.toResponse(treatment);

        return ResponseEntity.ok(response);
    }
}
