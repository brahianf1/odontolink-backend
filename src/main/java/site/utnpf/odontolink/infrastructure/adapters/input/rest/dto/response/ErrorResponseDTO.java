package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO estándar para respuestas de error.
 * Proporciona información estructurada sobre errores en la API.
 */
public class ErrorResponseDTO {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details;

    /**
     * Identificador unico del incidente. Se rellena solo en errores 5xx para
     * que el cliente pueda citarlo al reportar el problema y soporte/operaciones
     * lo correlacione contra el log donde quedo registrada la traza completa.
     * No se expone en errores 4xx (esperados) para mantener la respuesta compacta.
     */
    private String traceId;

    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    // Getters y Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
