package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.util.List;

/**
 * DTO de respuesta paginada para el historial de mensajes.
 * Implementa CU012: paginación para no saturar la memoria del cliente en conversaciones largas.
 *
 * <p>Convención del API (P3):
 * <ul>
 *   <li>{@code page=0} devuelve los <b>más recientes</b> (estilo WhatsApp/Telegram).</li>
 *   <li>{@code page=N+1} retrocede al bloque más antiguo (scroll-up).</li>
 *   <li>El orden de los mensajes <i>dentro</i> de la página es <b>DESC por sentAt</b>
 *       (el más reciente de la página primero). El frontend los renderiza de abajo hacia
 *       arriba al hacer scroll-up.</li>
 *   <li>{@code hasNext = true} significa que hay mensajes <b>más antiguos</b> sin cargar;
 *       el frontend habilita el scroll-up mientras sea true.</li>
 *   <li>{@code hasPrevious = page > 0}: hay una página más reciente (menos común; el FE
 *       suele iniciar siempre desde page=0).</li>
 *   <li>{@code last} se conserva como sinónimo de {@code !hasNext} por compatibilidad.</li>
 * </ul>
 *
 * <p>Se devuelve un envoltorio propio en lugar de {@code org.springframework.data.domain.Page}
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
     * {@code true} cuando ya no hay más páginas siguientes que cargar (no quedan mensajes
     * más antiguos). Sinónimo: {@code !hasNext}. El frontend lo usa para deshabilitar el
     * scroll-up cuando llega al inicio del historial.
     */
    private boolean last;

    /**
     * {@code true} si todavía hay mensajes más antiguos por cargar (scroll-up posible).
     * Es {@code !last}. Lo exponemos explícitamente para que el contrato sea autoexplicativo.
     */
    private boolean hasNext;

    /**
     * {@code true} si hay una página más nueva que la actual ({@code page > 0}). Rara vez
     * relevante en la práctica (la página 0 ya son los más recientes), pero parte del
     * contrato estándar de paginación.
     */
    private boolean hasPrevious;

    public PagedChatMessagesResponseDTO() {
    }

    public PagedChatMessagesResponseDTO(List<ChatMessageResponseDTO> messages, int page, int size,
                                        long totalElements, int totalPages, boolean last,
                                        boolean hasNext, boolean hasPrevious) {
        this.messages = messages;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
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

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
