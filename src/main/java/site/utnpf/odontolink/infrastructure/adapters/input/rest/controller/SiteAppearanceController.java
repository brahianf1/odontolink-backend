package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase;
import site.utnpf.odontolink.application.port.in.ISiteAppearanceConfigUseCase.AppearanceSnapshot;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.SiteAppearanceResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.SiteAppearanceRestMapper;

import java.util.concurrent.TimeUnit;

/**
 * Endpoint PUBLICO de appearance: lo consume el landing del frontend antes
 * de que el usuario inicie sesion (no podriamos exigir token). La
 * autorizacion {@code permitAll()} se declara en {@code SecurityConfig}.
 *
 * <p>Soporta cache HTTP con ETag fuerte basado en {@code version} y
 * {@code Cache-Control: public, max-age=60, must-revalidate}. Esto
 * permite que browsers + CDN intermedios cacheen 60s y se enteren de
 * cambios del admin sin presion sobre la BD. {@code must-revalidate}
 * obliga al validador (ETag) cuando expira el TTL.
 *
 * <p>Si el cliente trae {@code If-None-Match} con el ETag actual,
 * devolvemos 304 sin body para ahorrar bandwidth.
 */
@RestController
@RequestMapping("/api/site-config")
@SecurityRequirement(name = "")
@Tag(name = "Configuracion publica del sitio",
        description = "Appearance global consumido por el landing sin sesion.")
public class SiteAppearanceController {

    private final ISiteAppearanceConfigUseCase useCase;

    public SiteAppearanceController(ISiteAppearanceConfigUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Obtener appearance global",
            description = "Endpoint publico (sin auth). Devuelve theme/font/mode actuales " +
                    "y, si el themeVariantId apunta a un custom slug, embebe la paleta " +
                    "completa para que el landing aplique sin un segundo round-trip. " +
                    "Soporta ETag + Cache-Control (60s).")
    @GetMapping("/appearance")
    public ResponseEntity<SiteAppearanceResponseDTO> getAppearance(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        AppearanceSnapshot snapshot = useCase.get();
        String etag = SiteAppearanceRestMapper.etagFor(snapshot.appearance().getVersion());
        CacheControl cacheControl = CacheControl.maxAge(60, TimeUnit.SECONDS)
                .cachePublic()
                .mustRevalidate();
        // RFC 7232: cuando If-None-Match coincide con el ETag actual, el
        // server responde 304 sin body. Mantenemos el ETag y Cache-Control
        // en la respuesta 304 para que el cliente sepa por cuanto re-cachear.
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(cacheControl)
                    .build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cacheControl)
                .body(SiteAppearanceRestMapper.toResponse(snapshot));
    }
}
