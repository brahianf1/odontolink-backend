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
import site.utnpf.odontolink.domain.exception.RateLimitExceededException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
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
     * Maneja excepciones de reglas de negocio inválidas.
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

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Maneja excepciones de operaciones no autorizadas.
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
