package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.dto.ChatPollResult;
import site.utnpf.odontolink.application.port.in.dto.ChatSessionView;
import site.utnpf.odontolink.application.port.in.dto.PagedMessages;
import site.utnpf.odontolink.application.port.in.dto.ReadReceipt;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatPollResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatReadReceiptDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatSessionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PagedChatMessagesResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper entre modelos de dominio/aplicación y DTOs REST del chat.
 *
 * @author OdontoLink Team
 */
public class ChatRestMapper {

    /** Preview del último mensaje: cortamos a 120 caracteres para no inflar el payload del inbox. */
    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 120;

    private ChatRestMapper() {
    }

    public static ChatSessionResponseDTO toChatSessionResponseDTO(ChatSession chatSession) {
        return toChatSessionResponseDTO(chatSession, 0L, null);
    }

    /**
     * Versión enriquecida: además de los datos básicos incluye unreadCount y preview del último mensaje.
     */
    public static ChatSessionResponseDTO toChatSessionResponseDTO(ChatSessionView view) {
        if (view == null) {
            return null;
        }
        return toChatSessionResponseDTO(view.getSession(), view.getUnreadCount(), view.getLastMessage());
    }

    private static ChatSessionResponseDTO toChatSessionResponseDTO(ChatSession chatSession,
                                                                   long unreadCount,
                                                                   ChatMessage lastMessage) {
        if (chatSession == null) {
            return null;
        }
        ChatSessionResponseDTO dto = new ChatSessionResponseDTO();
        dto.setId(chatSession.getId());
        dto.setCreatedAt(chatSession.getCreatedAt());

        if (chatSession.getPatient() != null) {
            dto.setPatientId(chatSession.getPatient().getId());
            if (chatSession.getPatient().getUser() != null) {
                dto.setPatientName(
                        chatSession.getPatient().getUser().getFirstName() + " "
                                + chatSession.getPatient().getUser().getLastName());
            }
        }

        if (chatSession.getPractitioner() != null) {
            dto.setPractitionerId(chatSession.getPractitioner().getId());
            if (chatSession.getPractitioner().getUser() != null) {
                dto.setPractitionerName(
                        chatSession.getPractitioner().getUser().getFirstName() + " "
                                + chatSession.getPractitioner().getUser().getLastName());
            }
        }

        // Metadatos del inbox (CU012)
        dto.setUnreadCount(unreadCount);
        if (lastMessage != null) {
            dto.setLastMessageAt(lastMessage.getSentAt());
            dto.setLastMessagePreview(truncate(lastMessage.getContent(), LAST_MESSAGE_PREVIEW_LENGTH));
        }

        // Estado de bloqueo (RF28)
        dto.setBlocked(chatSession.isBlocked());
        dto.setBlockedAt(chatSession.getBlockedAt());
        dto.setBlockedByRole(chatSession.getBlockedByRole());
        dto.setBlockReason(chatSession.getBlockReason());
        if (chatSession.getBlockedByUser() != null) {
            dto.setBlockedByUserId(chatSession.getBlockedByUser().getId());
        }

        return dto;
    }

    public static ChatMessageResponseDTO toChatMessageResponseDTO(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }
        ChatMessageResponseDTO dto = new ChatMessageResponseDTO();
        dto.setId(chatMessage.getId());
        dto.setContent(chatMessage.getContent());
        dto.setSentAt(chatMessage.getSentAt());
        dto.setReadAt(chatMessage.getReadAt());

        if (chatMessage.getChatSession() != null) {
            dto.setChatSessionId(chatMessage.getChatSession().getId());
        }
        if (chatMessage.getSender() != null) {
            dto.setSenderId(chatMessage.getSender().getId());
            dto.setSenderName(
                    chatMessage.getSender().getFirstName() + " "
                            + chatMessage.getSender().getLastName());
        }
        return dto;
    }

    public static PagedChatMessagesResponseDTO toPagedChatMessagesResponseDTO(PagedMessages page) {
        if (page == null) {
            return null;
        }
        List<ChatMessageResponseDTO> messages = page.getMessages().stream()
                .map(ChatRestMapper::toChatMessageResponseDTO)
                .collect(Collectors.toList());
        return new PagedChatMessagesResponseDTO(
                messages,
                page.getPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious(),
                page.getServerTime()
        );
    }

    public static ChatPollResponseDTO toChatPollResponseDTO(ChatPollResult result) {
        if (result == null) {
            return null;
        }
        List<ChatMessageResponseDTO> messages = result.getMessages().stream()
                .map(ChatRestMapper::toChatMessageResponseDTO)
                .collect(Collectors.toList());
        List<ChatReadReceiptDTO> receipts = result.getReadReceipts().stream()
                .map(ChatRestMapper::toReadReceiptDTO)
                .collect(Collectors.toList());
        return new ChatPollResponseDTO(messages, receipts, result.getServerTime());
    }

    public static ChatReadReceiptDTO toReadReceiptDTO(ReadReceipt rr) {
        if (rr == null) {
            return null;
        }
        return new ChatReadReceiptDTO(rr.getMessageId(), rr.getReadAt());
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}
