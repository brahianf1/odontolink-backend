package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.ChatMessage;

import java.time.Instant;
import java.util.List;

/**
 * Projection paginada de mensajes para evitar saturar memoria en chats largos (CU012).
 *
 * <p>No exponemos {@code org.springframework.data.domain.Page} en el puerto para mantener
 * la capa de aplicación independiente de Spring Data.
 *
 * <p>Convención del API:
 * <ul>
 *   <li>{@code page=0} es la página de los mensajes <b>más recientes</b> (orden DESC global).</li>
 *   <li>{@code page=N+1} retrocede en el tiempo (mensajes más antiguos).</li>
 *   <li>Dentro de cada página, los mensajes vienen ordenados <b>DESC por {@code sentAt}</b>
 *       con tie-break por {@code id DESC}. Garantiza orden estable cuando dos mensajes
 *       comparten {@code sentAt}.</li>
 *   <li>{@code serverTime} es el instante capturado por el servidor justo antes de leer
 *       la base. El FE lo usa como cursor de arranque para el polling subsiguiente
 *       ({@code ?since=}), evitando clock skew con el reloj local.</li>
 * </ul>
 *
 * @author OdontoLink Team
 */
public class PagedMessages {

    private final List<ChatMessage> messages;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final Instant serverTime;

    public PagedMessages(List<ChatMessage> messages, int page, int size, long totalElements, Instant serverTime) {
        this.messages = messages;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.serverTime = serverTime;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public Instant getServerTime() {
        return serverTime;
    }

    /**
     * {@code true} cuando esta es la última página existente (no hay más páginas siguientes
     * en la convención DESC: no hay mensajes <i>más antiguos</i> por cargar).
     */
    public boolean isLast() {
        return page + 1 >= totalPages;
    }

    /**
     * {@code true} si hay otra página después de la actual en el sentido del paginador
     * (es decir, hay mensajes <b>más antiguos</b> sin cargar). Útil para que el frontend
     * decida si seguir habilitando el scroll-up.
     */
    public boolean hasNext() {
        return !isLast();
    }

    /**
     * {@code true} si hay una página antes que la actual en el sentido del paginador
     * (mensajes <b>más nuevos</b> que los de esta página). En la práctica equivale a
     * {@code page > 0}; el frontend rara vez lo necesita porque la página 0 ya es la
     * de los más recientes, pero lo exponemos por contrato de paginación estándar.
     */
    public boolean hasPrevious() {
        return page > 0;
    }
}
