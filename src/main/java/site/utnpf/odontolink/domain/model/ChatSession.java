package site.utnpf.odontolink.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una sala de chat entre un paciente y un practicante.
 * Esta sesión se crea automáticamente cuando se establece una relación formal
 * entre paciente y practicante mediante la primera atención (RF27).
 *
 * Responsabilidades:
 * - Mantener la relación entre los participantes del chat
 * - Almacenar el historial de mensajes
 * - Registrar el timestamp de creación
 *
 * @author OdontoLink Team
 */
public class ChatSession {
    private Long id;

    /** Relación N-a-1: El paciente en el chat */
    private Patient patient;

    /** Relación N-a-1: El practicante en el chat */
    private Practitioner practitioner;

    /** Relación 1-a-N: La lista de mensajes en esta sesión */
    private List<ChatMessage> messages;

    /** Timestamp de creación de la sesión */
    private Instant createdAt;

    /**
     * Constructor sin argumentos (requerido por mappers de persistencia)
     */
    public ChatSession() {
        this.messages = new ArrayList<>();
        this.createdAt = Instant.now();
    }

    /**
     * Constructor para crear una nueva sesión de chat.
     * Se invoca automáticamente cuando se crea la primera atención entre un paciente y un practicante.
     *
     * @param patient El paciente participante en el chat
     * @param practitioner El practicante participante en el chat
     */
    public ChatSession(Patient patient, Practitioner practitioner) {
        this();
        this.patient = patient;
        this.practitioner = practitioner;
    }

    // Comportamientos del Dominio Rico

    /**
     * Añade un mensaje a la sesión de chat.
     * Este método mantiene la consistencia bidireccional entre ChatSession y ChatMessage.
     *
     * @param message El mensaje a añadir
     */
    public void addMessage(ChatMessage message) {
        if (message != null) {
            this.messages.add(message);
            message.setChatSession(this);
        }
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(Practitioner practitioner) {
        this.practitioner = practitioner;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}