package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatSessionResponseDTO;

/**
 * Mapper para convertir entre modelos de dominio de Chat y DTOs REST.
 *
 * Este mapper transforma los modelos de dominio (ChatSession, ChatMessage)
 * a DTOs optimizados para la respuesta HTTP, incluyendo información denormalizada
 * para facilitar el consumo del frontend.
 *
 * @author OdontoLink Team
 */
public class ChatRestMapper {

    private ChatRestMapper() {
        // Utility class
    }

    /**
     * Convierte una ChatSession de dominio a un DTO de respuesta.
     * Incluye nombres denormalizados de paciente y practicante para el frontend.
     *
     * @param chatSession La sesión de chat del dominio
     * @return DTO de respuesta con información de la sesión
     */
    public static ChatSessionResponseDTO toChatSessionResponseDTO(ChatSession chatSession) {
        if (chatSession == null) {
            return null;
        }

        ChatSessionResponseDTO dto = new ChatSessionResponseDTO();
        dto.setId(chatSession.getId());
        dto.setCreatedAt(chatSession.getCreatedAt());

        // Mapear información del paciente
        if (chatSession.getPatient() != null) {
            dto.setPatientId(chatSession.getPatient().getId());
            if (chatSession.getPatient().getUser() != null) {
                String patientName = chatSession.getPatient().getUser().getFirstName() + " " +
                                   chatSession.getPatient().getUser().getLastName();
                dto.setPatientName(patientName);
            }
        }

        // Mapear información del practicante
        if (chatSession.getPractitioner() != null) {
            dto.setPractitionerId(chatSession.getPractitioner().getId());
            if (chatSession.getPractitioner().getUser() != null) {
                String practitionerName = chatSession.getPractitioner().getUser().getFirstName() + " " +
                                        chatSession.getPractitioner().getUser().getLastName();
                dto.setPractitionerName(practitionerName);
            }
        }

        return dto;
    }

    /**
     * Convierte un ChatMessage de dominio a un DTO de respuesta.
     * Incluye el nombre del remitente denormalizado para el frontend.
     *
     * @param chatMessage El mensaje de chat del dominio
     * @return DTO de respuesta con información del mensaje
     */
    public static ChatMessageResponseDTO toChatMessageResponseDTO(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }

        ChatMessageResponseDTO dto = new ChatMessageResponseDTO();
        dto.setId(chatMessage.getId());
        dto.setContent(chatMessage.getContent());
        dto.setSentAt(chatMessage.getSentAt());

        // Mapear información de la sesión de chat
        if (chatMessage.getChatSession() != null) {
            dto.setChatSessionId(chatMessage.getChatSession().getId());
        }

        // Mapear información del remitente
        if (chatMessage.getSender() != null) {
            dto.setSenderId(chatMessage.getSender().getId());
            String senderName = chatMessage.getSender().getFirstName() + " " +
                              chatMessage.getSender().getLastName();
            dto.setSenderName(senderName);
        }

        return dto;
    }
}
