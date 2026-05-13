package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.ChatMessage;

import java.util.List;

/**
 * Projection paginada de mensajes para evitar saturar memoria en chats largos (CU012).
 *
 * No exponemos {@code org.springframework.data.domain.Page} en el puerto para mantener
 * la capa de aplicación independiente de Spring Data.
 *
 * Convención: los mensajes vienen ordenados DESC (más reciente primero), como en Telegram/WhatsApp.
 * El frontend los renderiza en orden inverso a medida que se cargan páginas anteriores.
 *
 * @author OdontoLink Team
 */
public class PagedMessages {

    private final List<ChatMessage> messages;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PagedMessages(List<ChatMessage> messages, int page, int size, long totalElements) {
        this.messages = messages;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
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

    public boolean isLast() {
        return page + 1 >= totalPages;
    }
}
