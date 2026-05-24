package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.INonWorkingDayUseCase;
import site.utnpf.odontolink.domain.model.NonWorkingDay;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.NonWorkingDayResponseDTO;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/non-working-days")
@Tag(name = "Non-Working Days", description = "Días no laborables (feriados nacionales, recesos, etc.)")
public class NonWorkingDayController {

    private final INonWorkingDayUseCase nonWorkingDayUseCase;

    public NonWorkingDayController(INonWorkingDayUseCase nonWorkingDayUseCase) {
        this.nonWorkingDayUseCase = nonWorkingDayUseCase;
    }

    @GetMapping
    @Operation(summary = "Obtener días no laborables por año",
               description = "Devuelve los días no laborables para el año indicado. " +
                       "Si no se especifica año, devuelve los del año actual.")
    public ResponseEntity<List<NonWorkingDayResponseDTO>> getByYear(
            @Parameter(description = "Año a consultar (ej. 2026). Default: año actual.")
            @RequestParam(required = false) Integer year) {

        int effectiveYear = (year != null) ? year : Year.now().getValue();
        List<NonWorkingDay> days = nonWorkingDayUseCase.getByYear(effectiveYear);

        List<NonWorkingDayResponseDTO> response = days.stream()
                .map(d -> new NonWorkingDayResponseDTO(
                        d.getDate(),
                        d.getSource() != null ? d.getSource().name() : null,
                        d.getName(),
                        d.getType()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
