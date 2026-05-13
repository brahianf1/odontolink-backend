package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;

/**
 * Projection de la capa de aplicación que enriquece una ChatSession con metadatos derivados
 * (unreadCount, lastMessage) necesarios para construir el inbox del frontend sin acoplar el
 * dominio a preocupaciones de UI (CU012 paso 9).
 *
 * Es deliberadamente un record/POJO de la capa de aplicación: ni la capa de dominio ni los
 * adapters REST dependen de él, pero ambos pueden consumirlo (REST lo traduce a DTO HTTP).
 *
 * @author OdontoLink Team
 */
public class ChatSessionView {

    private final ChatSession session;
    private final long unreadCount;
    private final ChatMessage lastMessage;

    public ChatSessionView(ChatSession session, long unreadCount, ChatMessage lastMessage) {
        this.session = session;
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
    }

    public ChatSession getSession() {
        return session;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public ChatMessage getLastMessage() {
        return lastMessage;
    }
}
