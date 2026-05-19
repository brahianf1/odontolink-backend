package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase.AppearanceSnapshot;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateSiteAppearanceRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.SiteAppearanceResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.SiteAppearanceRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

/**
 * Endpoint admin para actualizar la appearance global. Defendido por
 * {@code @PreAuthorize("hasRole('ADMIN')")} ademas del catch-all en
 * {@code SecurityConfig} para mantener defensa en profundidad.
 *
 * <p>Optimistic locking via header {@code If-Match}. El cliente debe enviar
 * la {@code version} que vio en su ultimo GET; si no coincide con el estado
 * actual, lanzamos {@link VersionConflictException} (409). Aceptamos varios
 * formatos del header ({@code "v3"}, {@code v3}, {@code 3}) para ser
 * tolerantes con clientes que arman el header a mano.
 */
@RestController
@RequestMapping("/api/admin/site-config/appearance")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Administracion - Appearance del sitio",
        description = "Configuracion visual global (theme, font, mode).")
public class AdminSiteAppearanceController {

    private final ISiteAppearanceConfigUseCase useCase;
    private final AuthenticationFacade authFacade;

    public AdminSiteAppearanceController(ISiteAppearanceConfigUseCase useCase,
                                         AuthenticationFacade authFacade) {
        this.useCase = useCase;
        this.authFacade = authFacade;
    }

    @Operation(summary = "Actualizar appearance global",
            description = "Reemplaza por completo theme/font/mode. Requiere If-Match con la " +
                    "version vista en el ultimo GET; si la version cambio entre tanto, " +
                    "devuelve 409 VERSION_CONFLICT y el cliente debe recargar.")
    @PutMapping
    public ResponseEntity<SiteAppearanceResponseDTO> updateAppearance(
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateSiteAppearanceRequestDTO body) {
        int expectedVersion = parseIfMatch(ifMatch);
        Long actorId = authFacade.getAuthenticatedUser().getId();
        AppearanceSnapshot updated = useCase.update(
                SiteAppearanceRestMapper.toCommand(body),
                expectedVersion,
                actorId
        );
        String etag = SiteAppearanceRestMapper.etagFor(updated.appearance().getVersion());
        return ResponseEntity.ok()
                .eTag(etag)
                .body(SiteAppearanceRestMapper.toResponse(updated));
    }

    /**
     * Parsea el header {@code If-Match} aceptando varios formatos. Si no
     * matchea ninguno, considera la request stale: es la forma mas segura
     * de no permitir un PUT sin guardrail de version. El cliente recibe
     * 409 con la version actual para que pueda recargar y reintentar.
     */
    private int parseIfMatch(String header) {
        if (header == null || header.isBlank()) {
            // Sin If-Match no es seguro proceder: hay que forzar al cliente
            // a sincronizar. Lanzamos con currentVersion=-1 (sentinel) y el
            // mapper en el handler pondra "currentVersion: -1" en details.
            // Esto deja claro en el body que el header faltaba.
            int currentVersion = useCase.get().appearance().getVersion();
            throw new VersionConflictException(currentVersion);
        }
        String stripped = header.trim();
        // Quitar comillas circundantes si vienen (ETag estandar las usa).
        if (stripped.startsWith("\"") && stripped.endsWith("\"") && stripped.length() >= 2) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        // Quitar prefijo W/ de weak ETag.
        if (stripped.startsWith("W/")) {
            stripped = stripped.substring(2).trim();
            if (stripped.startsWith("\"") && stripped.endsWith("\"") && stripped.length() >= 2) {
                stripped = stripped.substring(1, stripped.length() - 1);
            }
        }
        // Quitar prefijo 'v' opcional (formato "v3").
        if (stripped.startsWith("v") || stripped.startsWith("V")) {
            stripped = stripped.substring(1);
        }
        try {
            return Integer.parseInt(stripped);
        } catch (NumberFormatException ex) {
            int currentVersion = useCase.get().appearance().getVersion();
            throw new VersionConflictException(currentVersion);
        }
    }
}
