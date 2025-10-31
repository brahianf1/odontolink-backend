package site.utnpf.odontolink.domain.model;

import java.util.List;

/**
 * Representa una sala de Chat entre un paciente y un practicante.
 * Esta sesión se crea cuando existe una relación de atención.
 */
public class ChatSession {
    private Long id;

    /** Relación N-a-1: El paciente en el chat */
    private Patient patient;

    /** Relación N-a-1: El practicante en el chat */
    private Practitioner practitioner;

    /** Relación 1-a-N: La lista de mensajes en esta sesión */
    private List<ChatMessage> messages;
}