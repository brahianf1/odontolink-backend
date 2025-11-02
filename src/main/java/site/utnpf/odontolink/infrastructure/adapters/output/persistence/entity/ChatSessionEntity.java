package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla 'chat_sessions'.
 * Representa una sesión de chat entre un paciente y un practicante en la base de datos.
 *
 * Esta entidad modela RF26, RF27 - CU 6.1, 6.2, 6.3: Chat interno paciente-practicante.
 * Una sesión de chat se crea automáticamente cuando se establece la primera atención.
 *
 * @author OdontoLink Team
 */
@Entity
@Table(name = "chat_sessions",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_chat_session_patient_practitioner",
                columnNames = {"patient_id", "practitioner_id"}
            )
        },
        indexes = {
            @Index(name = "idx_chat_session_patient", columnList = "patient_id"),
            @Index(name = "idx_chat_session_practitioner", columnList = "practitioner_id")
        })
public class ChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación ManyToOne con PatientEntity.
     * Una sesión de chat pertenece a un paciente específico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    /**
     * Relación ManyToOne con PractitionerEntity.
     * Una sesión de chat pertenece a un practicante específico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerEntity practitioner;

    /**
     * Relación OneToMany con ChatMessageEntity (bidireccional).
     *
     * CascadeType.ALL: Todas las operaciones (persist, merge, remove, etc.) se propagan a los mensajes.
     * orphanRemoval = true: Si un mensaje se remueve de la lista, se elimina de la BD.
     *
     * mappedBy = "chatSession": Indica que ChatMessageEntity tiene el lado owner de la relación.
     */
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessageEntity> messages = new ArrayList<>();

    /**
     * Timestamp de creación de la sesión.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Constructor sin argumentos (requerido por JPA)
     */
    public ChatSessionEntity() {
        this.createdAt = Instant.now();
    }

    /**
     * Callback de JPA ejecutado antes de persistir la entidad.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Método de utilidad para mantener la consistencia bidireccional.
     * Agrega un ChatMessageEntity a la lista y establece la relación inversa.
     *
     * @param message El mensaje a agregar
     */
    public void addMessage(ChatMessageEntity message) {
        this.messages.add(message);
        message.setChatSession(this);
    }

    /**
     * Método de utilidad para mantener la consistencia bidireccional.
     * Remueve un ChatMessageEntity de la lista y rompe la relación inversa.
     *
     * @param message El mensaje a remover
     */
    public void removeMessage(ChatMessageEntity message) {
        this.messages.remove(message);
        message.setChatSession(null);
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public PractitionerEntity getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(PractitionerEntity practitioner) {
        this.practitioner = practitioner;
    }

    public List<ChatMessageEntity> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageEntity> messages) {
        this.messages = messages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
