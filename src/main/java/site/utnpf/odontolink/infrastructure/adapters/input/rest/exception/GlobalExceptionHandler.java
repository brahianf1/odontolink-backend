package site.utnpf.odontolink.infrastructure.adapters.input.rest.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import site.utnpf.odontolink.application.port.out.StorageException;
import site.utnpf.odontolink.domain.exception.AuthenticationFailedException;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.exception.IncorrectCurrentPasswordException;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.InvalidPasswordResetTokenException;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.domain.exception.RateLimitExceededException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.ThemeInUseException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.exception.VersionConflictException;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ErrorResponseDTO;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para la API REST.
 * Centraliza el manejo de errores y proporciona respuestas consistentes.
 * Sigue las mejores prácticas de Spring Boot con @ControllerAdvice.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de validación de Bean Validation (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.toList());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Los datos proporcionados no son válidos",
                request.getRequestURI()
        );
        errorResponse.setDetails(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Maneja excepciones de recursos duplicados (email, DNI, etc.).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Duplicate Resource",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Maneja excepciones de recursos no encontrados.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Maneja excepciones de reglas de negocio inválidas. Propaga el {@code errorCode}
     * si la excepción lo definió y, si el dominio adjunto una lista de
     * {@code details} estructurada (codigos estables), tambien la expone en el
     * body para que el frontend pueda ramificar UX por cada causa concreta
     * sin parsear el mensaje humano.
     */
    @ExceptionHandler(InvalidBusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidBusinessRuleException(
            InvalidBusinessRuleException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Business Rule Violation",
                ex.getMessage(),
                request.getRequestURI()
        );
        errorResponse.setErrorCode(ex.getErrorCode());
        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            errorResponse.setDetails(ex.getDetails());
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Maneja excepciones de operaciones no autorizadas. Propaga el {@code errorCode}
     * para distinguir motivos distintos del mismo 403 (p. ej. {@code CHAT_BLOCKED}
     * vs {@code CHAT_NOT_PARTICIPANT}).
     */
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedOperationException(
            UnauthorizedOperationException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI()
        );
        errorResponse.setErrorCode(ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Maneja excepciones de acceso denegado por Spring Security.
     * Se lanza cuando un usuario autenticado intenta acceder a un recurso
     * para el cual no tiene los permisos necesarios (@PreAuthorize).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "No tiene permisos para realizar esta operación",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Maneja excepciones de token de recuperación inválido (RF04).
     *
     * Se devuelve HTTP 400 con un mensaje genérico y no detalla si el motivo
     * fue expiración, consumo previo o inexistencia, evitando filtrar
     * información útil para un atacante que intente adivinar tokens.
     */
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPasswordResetTokenException(
            InvalidPasswordResetTokenException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Password Reset Token",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Maneja {@link RateLimitExceededException} disparado por servicios
     * (no por el filtro: el filtro escribe directamente la respuesta sin
     * pasar por aqui).
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleRateLimitExceededException(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                ex.getMessage(),
                request.getRequestURI()
        );

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (ex.getRetryAfterSeconds() != null) {
            builder.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return builder.body(errorResponse);
    }

    /**
     * Maneja el limite de tamanio del subsistema multipart de Spring. La
     * cota propia del use case de fotos (max-bytes) se valida despues y
     * devuelve 422 con mensaje especifico; este handler atrapa el corte
     * mas grueso de Spring antes de que llegue al controller.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Payload Too Large",
                "El archivo subido excede el tamanio maximo permitido.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Maneja fallas del proveedor externo del LLM (modulo de IA, RF31-RF33).
     * Devolvemos 503 porque las causas son siempre externas (timeouts del
     * proveedor, credencial expirada, UUID inexistente), y el frontend
     * puede sugerir reintentar / forzar resync. El {@code errorCode} viaja
     * para que el FE distinga la causa (unavailable vs bad_request).
     */
    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<ErrorResponseDTO> handleLlmProviderException(
            LlmProviderException ex,
            HttpServletRequest request) {

        log.warn("Falla del subsistema de IA al servir {} {} (status={}, code={}): {}",
                request.getMethod(), request.getRequestURI(),
                ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());

        // El statusCode de LlmProviderException puede mapear a HTTP distintos
        // segun el errorCode: la clase la usamos tambien para flujos del
        // chatbot (disabled/forbidden/not-published) que no son "el proveedor
        // esta caido", asi que el mapeo tiene que considerar el code.
        String code = ex.getErrorCode();
        HttpStatus status;
        String title;
        String message;
        if (site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes.AI_AGENT_DISABLED.equals(code)
                || site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes.AI_AGENT_ACCESS_DENIED.equals(code)) {
            status = HttpStatus.FORBIDDEN;
            title = "Access Denied";
            message = ex.getMessage();
        } else if (site.utnpf.odontolink.infrastructure.adapters.input.rest.error.AiAgentErrorCodes.AI_AGENT_ANONYMOUS_FORBIDDEN.equals(code)) {
            status = HttpStatus.UNAUTHORIZED;
            title = "Authentication Required";
            message = ex.getMessage();
        } else {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            title = "AI Provider Unavailable";
            message = "El servicio de IA no esta disponible temporalmente. Intente nuevamente.";
        }

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                status.value(),
                title,
                message,
                request.getRequestURI()
        );
        errorResponse.setErrorCode(ex.getErrorCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Maneja fallas del adaptador de object storage. Devolvemos 503 porque
     * en general son fallas de infraestructura externa (credenciales,
     * conectividad al bucket); el frontend puede sugerir reintentar.
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponseDTO> handleStorageException(
            StorageException ex,
            HttpServletRequest request) {

        log.warn("Falla del object storage al servir {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Storage Unavailable",
                "El servicio de almacenamiento no esta disponible temporalmente. Intente nuevamente.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Maneja {@link IncorrectCurrentPasswordException}: ocurre cuando el usuario
     * autenticado provee una {@code currentPassword} incorrecta en el cambio de
     * contrasenia. Devolvemos 422 (no 401) porque el token sigue siendo valido;
     * lo que fallo es la verificacion de identidad adicional sobre el payload.
     * El error code {@code "Incorrect Current Password"} permite al frontend
     * distinguirlo del 401 generico (login fallido) y evitar disparar auto-logout.
     */
    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    public ResponseEntity<ErrorResponseDTO> handleIncorrectCurrentPasswordException(
            IncorrectCurrentPasswordException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Incorrect Current Password",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Maneja excepciones de autenticación fallida.
     */
    @ExceptionHandler({AuthenticationFailedException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed",
                "Credenciales inválidas",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Maneja {@link VersionConflictException}: el cliente envio un PUT con
     * version desactualizada (header {@code If-Match} stale o ausente).
     * Devuelve 409 con {@code errorCode=VERSION_CONFLICT} y la version
     * actual del recurso en {@code details[]} para que el FE pueda recargar
     * y reintentar sin un GET extra.
     */
    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleVersionConflictException(
            VersionConflictException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Version Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        errorResponse.setErrorCode(ex.getErrorCode());
        errorResponse.setDetails(List.of("currentVersion: " + ex.getCurrentVersion()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Maneja {@link ThemeInUseException}: se intento borrar un custom theme
     * que esta seteado como appearance activa. Devuelve 409 con
     * {@code errorCode=THEME_IN_USE} y el slug en {@code details[]} para que
     * el FE pueda decirle al admin que cambie el theme global primero.
     */
    @ExceptionHandler(ThemeInUseException.class)
    public ResponseEntity<ErrorResponseDTO> handleThemeInUseException(
            ThemeInUseException ex,
            HttpServletRequest request) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Theme In Use",
                ex.getMessage(),
                request.getRequestURI()
        );
        errorResponse.setErrorCode(ex.getErrorCode());
        errorResponse.setDetails(List.of("slug: " + ex.getSlug()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Catch-all para cualquier excepcion que no haya sido manejada por un
     * handler especifico. Hasta esta version el handler respondia 500 sin
     * loguear nada, lo que significaba que cualquier bug latente (p.ej.
     * LazyInitializationException tras desactivar OSIV) quedaba sepultado en
     * silencio. Esa garantia se rompe explicitamente aqui:
     *
     *  - Se genera un {@code traceId} unico por incidente.
     *  - Se loguea a nivel ERROR con la traza completa, el verbo HTTP y la
     *    ruta solicitada para correlacionar de inmediato contra el cliente.
     *  - El {@code traceId} viaja en la respuesta para que el frontend pueda
     *    citarlo al reportar el problema, sin filtrar nunca el detalle del
     *    error real (defensa contra disclosure de internals).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();

        log.error("[traceId={}] Excepcion no controlada en {} {} - {}: {}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getName(),
                ex.getMessage(),
                ex);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ha ocurrido un error interno en el servidor",
                request.getRequestURI()
        );
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
