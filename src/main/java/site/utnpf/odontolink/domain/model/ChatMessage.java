package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Representa un único mensaje dentro de una ChatSession.
 */
public class ChatMessage {
    private Long id;

    /** Relación N-a-1: La sesión a la que pertenece este mensaje */
    private ChatSession chatSession;

    /** Relación N-a-1: El User (Paciente o Practicante) que envió el mensaje */
    private User sender;

    private String content;
    private Instant sentAt;
}