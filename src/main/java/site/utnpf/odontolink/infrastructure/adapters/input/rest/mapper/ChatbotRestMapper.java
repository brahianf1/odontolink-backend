package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.dto.ChatbotPublicInfo;
import site.utnpf.odontolink.domain.model.ChatbotInteractionResult;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatbotMessageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ChatbotPublicInfoResponseDTO;

/**
 * Mappers REST del chatbot (RF29/RF31/RF32/RF34).
 */
public final class ChatbotRestMapper {

    private ChatbotRestMapper() {
    }

    public static ChatbotPublicInfoResponseDTO toResponse(ChatbotPublicInfo info) {
        ChatbotPublicInfoResponseDTO dto = new ChatbotPublicInfoResponseDTO();
        dto.setAccessGranted(info.accessGranted());
        dto.setAccessMode(info.accessMode());
        dto.setDisplayName(info.displayName());
        dto.setWelcomeMessage(info.welcomeMessage());
        dto.setDenyReason(info.denyReason());
        return dto;
    }

    public static ChatbotMessageResponseDTO toResponse(ChatbotInteractionResult result) {
        ChatbotMessageResponseDTO dto = new ChatbotMessageResponseDTO();
        dto.setSessionId(result.sessionId());
        dto.setAnonymousToken(result.anonymousToken());
        dto.setReply(result.reply());
        dto.setConfidence(result.confidence());
        dto.setBasedOnKnowledgeBase(result.basedOnKnowledgeBase());
        dto.setEmergencyDetected(result.emergencyDetected());
        dto.setPiiBlocked(result.piiBlocked());
        dto.setDetectedPiiTypes(result.detectedPiiTypes());
        dto.setFallbackTriggered(result.fallbackTriggered());
        dto.setLatencyMs(result.latencyMs());
        dto.setRetrievedDocumentIds(result.retrievedDocumentIds());
        return dto;
    }
}
