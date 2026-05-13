package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.util.List;

/**
 * DTO de respuesta paginada para el historial de mensajes.
 * Implementa CU012: paginación para no saturar la memoria del cliente en conversaciones largas.
 *
 * Convención del API:
 *  - Los mensajes vienen ordenados DESC (más reciente primero), igual que WhatsApp/Telegram.
 *  - El frontend hace "scroll infinito hacia arriba" pidiendo la siguiente página (page+1).
 *  - El campo 'last' indica si ya no quedan páginas anteriores que cargar.
 *
 * Se devuelve un envoltorio propio en lugar de {@code org.springframework.data.domain.Page}
 * para no acoplar la API HTTP a una serialización específica de Spring Data.
 *
 * @author OdontoLink Team
 */
public class PagedChatMessagesResponseDTO {

    /**
     * Mensajes de la página actual, ordenados DESC por sentAt (más reciente primero).
     */
    private List<ChatMessageResponseDTO> messages;

    /**
     * Número de página actual (base 0).
     */
    private int page;

    /**
     * Tamaño de página utilizado. Entre 1 y 200 (validado en el servicio).
     */
    private int size;

    /**
     * Total de mensajes en la sesión. Permite al frontend mostrar contadores tipo "1 / 312".
     */
    private long totalElements;

    /**
     * Total de páginas calculado como ceil(totalElements / size).
     */
    private int totalPages;

    /**
     * true cuando ya no hay páginas siguientes que cargar (la actual fue la última histórica).
     * El frontend lo usa para deshabilitar el scroll-up cuando llega al inicio del historial.
     */
    private boolean last;

    // Constructores
    public PagedChatMessagesResponseDTO() {
    }

    public PagedChatMessagesResponseDTO(List<ChatMessageResponseDTO> messages, int page, int size,
                                        long totalElements, int totalPages, boolean last) {
        this.messages = messages;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    // Getters y Setters
    public List<ChatMessageResponseDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageResponseDTO> messages) {
        this.messages = messages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
