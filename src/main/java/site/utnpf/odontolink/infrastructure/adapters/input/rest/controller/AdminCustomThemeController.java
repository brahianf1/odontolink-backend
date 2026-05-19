package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.ICustomThemeAdminUseCase;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.domain.model.CustomTheme;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CreateCustomThemeRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateCustomThemeRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.CustomThemeResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.CustomThemeSummaryResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.CustomThemeRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.util.List;

/**
 * CRUD admin de custom themes. Defendido por
 * {@code @PreAuthorize("hasRole('ADMIN')")} y catch-all en
 * {@code SecurityConfig}.
 *
 * <p>El listado responde {@code Cache-Control: no-store}: el admin necesita
 * ver el estado actual al instante despues de cualquier cambio.
 */
@RestController
@RequestMapping("/api/admin/site-config/custom-themes")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Custom themes",
        description = "Alta/edicion/baja logica de themes custom (paletas pegadas por el admin).")
public class AdminCustomThemeController {

    private final ICustomThemeAdminUseCase useCase;
    private final AuthenticationFacade authFacade;

    public AdminCustomThemeController(ICustomThemeAdminUseCase useCase,
                                      AuthenticationFacade authFacade) {
        this.useCase = useCase;
        this.authFacade = authFacade;
    }

    @Operation(summary = "Listar custom themes activos",
            description = "Excluye soft-deleted, ordena por createdAt DESC. Omite sourceCss " +
                    "del payload (disponible en el GET de detalle).")
    @GetMapping
    public ResponseEntity<List<CustomThemeSummaryResponseDTO>> list() {
        List<CustomTheme> themes = useCase.list();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CustomThemeRestMapper.toSummaryList(themes));
    }

    @Operation(summary = "Obtener detalle de un custom theme (incluye sourceCss)")
    @GetMapping("/{id}")
    public ResponseEntity<CustomThemeResponseDTO> getById(@PathVariable Long id) {
        CustomTheme theme = useCase.getById(id);
        return ResponseEntity.ok(CustomThemeRestMapper.toResponse(theme));
    }

    @Operation(summary = "Crear custom theme",
            description = "El slug se autogenera a partir del name. Los maps light/dark deben " +
                    "tener exactamente las 35 keys del contrato y valores en formato #rrggbb.")
    @PostMapping
    public ResponseEntity<CustomThemeResponseDTO> create(@Valid @RequestBody CreateCustomThemeRequestDTO body) {
        Long actorId = authFacade.getAuthenticatedUser().getId();
        CustomTheme created = useCase.create(CustomThemeRestMapper.toCreateCommand(body), actorId);
        return ResponseEntity.status(201).body(CustomThemeRestMapper.toResponse(created));
    }

    @Operation(summary = "Actualizar custom theme",
            description = "Requiere If-Match con la version vista en el ultimo GET. Si la " +
                    "version cambio entre tanto, devuelve 409 VERSION_CONFLICT. El slug NO es " +
                    "editable; para cambiarlo, crea un theme nuevo y borra el viejo.")
    @PutMapping("/{id}")
    public ResponseEntity<CustomThemeResponseDTO> update(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateCustomThemeRequestDTO body) {
        int expectedVersion = parseIfMatchOrConflict(id, ifMatch);
        Long actorId = authFacade.getAuthenticatedUser().getId();
        CustomTheme updated = useCase.update(
                id,
                CustomThemeRestMapper.toUpdateCommand(body),
                expectedVersion,
                actorId
        );
        return ResponseEntity.ok(CustomThemeRestMapper.toResponse(updated));
    }

    @Operation(summary = "Eliminar (soft delete) un custom theme",
            description = "Si el theme esta seteado como appearance activa, devuelve 409 " +
                    "THEME_IN_USE: el admin debe cambiar el theme global primero.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        Long actorId = authFacade.getAuthenticatedUser().getId();
        useCase.softDelete(id, actorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Misma logica de parseo que en {@code AdminSiteAppearanceController}.
     * Centralizarlo en un helper compartido fue tentador, pero los dos
     * usos divergen en que aca necesitamos consultar el id para sacar la
     * currentVersion del theme especifico, mientras que la appearance es
     * un singleton sin id en la URL. Mantenerlo duplicado y obvio gana.
     */
    private int parseIfMatchOrConflict(Long id, String header) {
        if (header == null || header.isBlank()) {
            int current = useCase.getById(id).getVersion();
            throw new VersionConflictException(current);
        }
        String stripped = header.trim();
        if (stripped.startsWith("\"") && stripped.endsWith("\"") && stripped.length() >= 2) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        if (stripped.startsWith("W/")) {
            stripped = stripped.substring(2).trim();
            if (stripped.startsWith("\"") && stripped.endsWith("\"") && stripped.length() >= 2) {
                stripped = stripped.substring(1, stripped.length() - 1);
            }
        }
        if (stripped.startsWith("v") || stripped.startsWith("V")) {
            stripped = stripped.substring(1);
        }
        try {
            return Integer.parseInt(stripped);
        } catch (NumberFormatException ex) {
            int current = useCase.getById(id).getVersion();
            throw new VersionConflictException(current);
        }
    }
}
