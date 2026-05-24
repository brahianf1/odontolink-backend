package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IFeedbackCriterionCatalogUseCase;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.FeedbackCriterionDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Expone el catálogo de criterios para que el frontend renderice
 * dinámicamente la encuesta. Diseñado para ser code-driven: el FE NO
 * hardcodea los criterios; los pide por dirección y los muestra en el
 * orden indicado por {@code displayOrder}.
 */
@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback - Catálogo", description = "Catálogo de criterios para la encuesta multi-criterio.")
@SecurityRequirement(name = "Bearer Authentication")
public class FeedbackCriterionCatalogController {

    private final IFeedbackCriterionCatalogUseCase catalogUseCase;

    public FeedbackCriterionCatalogController(IFeedbackCriterionCatalogUseCase catalogUseCase) {
        this.catalogUseCase = catalogUseCase;
    }

    @Operation(
            summary = "Listar criterios activos por dirección",
            description = "Devuelve los criterios activos aplicables a la dirección del feedback, " +
                    "ordenados por `displayOrder` ascendente. Es la fuente de verdad para que el " +
                    "frontend pinte los inputs de la encuesta. Refetch sugerido cuando se cambia de " +
                    "vista (paciente vs practicante) o al perder caché local."
    )
    @GetMapping("/criteria")
    @PreAuthorize("hasAnyRole('PATIENT', 'PRACTITIONER', 'SUPERVISOR')")
    public ResponseEntity<List<FeedbackCriterionDTO>> listCriteria(
            @Parameter(description = "Dirección del feedback. Default: PATIENT_TO_PRACTITIONER.",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = FeedbackDirection.class))
            @RequestParam(name = "direction", required = false,
                    defaultValue = "PATIENT_TO_PRACTITIONER") FeedbackDirection direction) {
        List<FeedbackCriterionDTO> body = catalogUseCase.listActiveForDirection(direction).stream()
                .map(FeedbackCriterionDTO::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
