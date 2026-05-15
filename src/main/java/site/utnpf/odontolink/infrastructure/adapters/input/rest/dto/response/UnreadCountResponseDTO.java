package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

/**
 * DTO de respuesta para el endpoint global de no-leídos.
 *
 * <p>Lo expone {@code GET /api/chat/unread-count}: alimenta el badge global del sidebar/AppBar
 * sin obligar al frontend a recorrer todas las sesiones del inbox. Una sola query SQL agregada.
 *
 * @author OdontoLink Team
 */
public class UnreadCountResponseDTO {

    /**
     * Suma de mensajes no leídos en todas las sesiones (paciente o practicante) donde el
     * usuario autenticado es participante. Excluye sus propios mensajes.
     */
    private long total;

    public UnreadCountResponseDTO() {
    }

    public UnreadCountResponseDTO(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
