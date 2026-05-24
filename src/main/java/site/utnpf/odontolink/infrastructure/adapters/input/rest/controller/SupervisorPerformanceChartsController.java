package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase.PractitionerCriterionChartResult;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase.PractitionerRankingChartResult;
import site.utnpf.odontolink.application.port.in.dto.PractitionerCriterionChartQuery;
import site.utnpf.odontolink.application.port.in.dto.PractitionerRankingChartQuery;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PractitionerCriterionChartResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PractitionerRankingChartResponseDTO;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.LocalDate;

/**
 * Charts del panel docente: top-N practicantes por criterio y ranking
 * combinado. Cerco supervisor→practicantes aplicado siempre; sólo aparecen
 * practicantes con al menos {@code RANKING_MIN_FEEDBACK_COUNT} feedbacks.
 */
@RestController
@RequestMapping("/api/supervisors/feedbacks/charts")
@Tag(name = "Supervisor - Charts de Feedback",
        description = "Gráficos para el panel docente: top-N por criterio y ranking combinado.")
@SecurityRequirement(name = "Bearer Authentication")
public class SupervisorPerformanceChartsController {

    private final IPractitionerPerformanceUseCase performanceUseCase;
    private final AuthenticationFacade authenticationFacade;

    public SupervisorPerformanceChartsController(IPractitionerPerformanceUseCase performanceUseCase,
                                                 AuthenticationFacade authenticationFacade) {
        this.performanceUseCase = performanceUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    @Operation(
            summary = "Top-N practicantes por un criterio puntual",
            description = "Devuelve los mejores practicantes según el promedio de un único criterio. " +
                    "El parámetro `criterionCode` se obtiene del catálogo " +
                    "(`GET /api/feedback/criteria`). Sólo aparecen practicantes con al menos " +
                    "`RANKING_MIN_FEEDBACK_COUNT` scores sobre ese criterio. Si el supervisor no " +
                    "tiene practicantes a cargo o ninguno cumple el umbral, `entries` viene vacío."
    )
    @GetMapping("/top-by-criterion")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<PractitionerCriterionChartResponseDTO> topByCriterion(
            @Parameter(description = "Código del criterio. Ej: PUNCTUALITY", required = true)
            @RequestParam("criterionCode") String criterionCode,
            @Parameter(description = "Tamaño máximo del top. Si se omite, usa el default del sistema.")
            @RequestParam(value = "topN", required = false) Integer topN,
            @Parameter(description = "Filtra feedbacks con createdAt >= startDate (UTC)")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filtra feedbacks con createdAt <= endDate (UTC)")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Restringe a un tratamiento puntual")
            @RequestParam(value = "treatmentId", required = false) Long treatmentId) {

        User supervisor = authenticationFacade.getAuthenticatedUser();
        PractitionerCriterionChartQuery query = new PractitionerCriterionChartQuery(
                criterionCode, topN, startDate, endDate, treatmentId);
        PractitionerCriterionChartResult result = performanceUseCase.getTopByCriterion(query, supervisor);
        return ResponseEntity.ok(PractitionerCriterionChartResponseDTO.fromResult(result));
    }

    @Operation(
            summary = "Ranking combinado de practicantes",
            description = "Calcula el promedio de promedios por practicante sobre los criterios " +
                    "marcados `includeInRanking=true` para la dirección PATIENT_TO_PRACTITIONER. " +
                    "Cada criterio pesa igual sin importar su sample size. Sólo aparecen practicantes " +
                    "con al menos `RANKING_MIN_FEEDBACK_COUNT` feedbacks distintos contribuyendo."
    )
    @GetMapping("/practitioners-ranking")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<PractitionerRankingChartResponseDTO> practitionersRanking(
            @Parameter(description = "Tamaño máximo del ranking. Si se omite, usa el default del sistema.")
            @RequestParam(value = "topN", required = false) Integer topN,
            @Parameter(description = "Filtra feedbacks con createdAt >= startDate")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filtra feedbacks con createdAt <= endDate")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Restringe a un tratamiento puntual")
            @RequestParam(value = "treatmentId", required = false) Long treatmentId) {

        User supervisor = authenticationFacade.getAuthenticatedUser();
        PractitionerRankingChartQuery query = new PractitionerRankingChartQuery(
                topN, startDate, endDate, treatmentId);
        PractitionerRankingChartResult result = performanceUseCase.getOverallRanking(query, supervisor);
        return ResponseEntity.ok(PractitionerRankingChartResponseDTO.fromResult(result));
    }
}
