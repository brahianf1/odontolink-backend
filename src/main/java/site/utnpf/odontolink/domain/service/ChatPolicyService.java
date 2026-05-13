package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;

/**
 * Servicio de Dominio que implementa las políticas y reglas de negocio del chat interno (RF26/RF27/RF28).
 *
 * Es el "Rulebook" del chat: agrupa todas las validaciones de pertenencia, autoridad y bloqueo
 * para que los Servicios de Aplicación no las dispersen.
 *
 * @author OdontoLink Team
 */
public class ChatPolicyService {

    /**
     * Valida que el remitente puede enviar un mensaje en la sesión.
     *
     * Reglas:
     * 1. RF26: Solo el paciente o el practicante de la sesión pueden enviar mensajes.
     * 2. RF28: Si la sesión está bloqueada, el paciente queda silenciado (el practicante sí puede
     *    seguir documentando).
     */
    public void validateMessageSend(ChatSession session, User sender) {
        validateParticipantAccess(session, sender, "El usuario no pertenece a esta sesión de chat.");
        session.ensureSenderCanWrite(sender);
    }

    /**
     * Valida que un usuario puede acceder (leer) los mensajes de una sesión.
     * Aún si está bloqueada, los participantes pueden ver el historial: el bloqueo silencia, no
     * borra el contexto clínico.
     */
    public void validateMessageAccess(ChatSession session, User user) {
        validateParticipantAccess(session, user, "El usuario no pertenece a esta sesión de chat.");
    }

    /**
     * Valida que el actor puede ejecutar un bloqueo (RF28).
     *
     * Reglas:
     * - Solo un Practitioner (rol) puede bloquear.
     * - El Practitioner debe ser el practicante de la sesión.
     * - La autorización por rol también se enforce vía @PreAuthorize en el controlador,
     *   pero la duplicamos a nivel dominio para que la regla viva en el Rulebook y no se
     *   pueda saltear desde otros adaptadores.
     */
    public void validateBlockOperation(ChatSession session, User actor) {
        if (session == null || actor == null) {
            throw new IllegalArgumentException("La sesión de chat y el actor no pueden ser nulos.");
        }
        if (actor.getRole() != Role.ROLE_PRACTITIONER) {
            throw new UnauthorizedOperationException("Solo un practicante puede bloquear una sesión de chat.");
        }
        if (!isPractitioner(session, actor)) {
            throw new UnauthorizedOperationException("El practicante no pertenece a esta sesión de chat.");
        }
    }

    /**
     * Valida que el actor puede desbloquear la sesión (RF28).
     *
     * Política consensuada: lo puede revertir cualquier Practitioner de la sesión —en la práctica
     * el practicante de la sesión es único, así que esto equivale a "el dueño clínico del caso".
     * Mantiene la simetría con el bloqueo y conserva el audit trail original mientras estuvo activo.
     */
    public void validateUnblockOperation(ChatSession session, User actor) {
        validateBlockOperation(session, actor);
    }

    /**
     * Valida que el usuario puede marcar mensajes como leídos en la sesión (CU012).
     *
     * Reglas:
     * - El usuario debe pertenecer a la sesión.
     * - Solo se pueden marcar como leídos mensajes en los que el usuario es el RECEPTOR
     *   (no se puede marcar como leído un mensaje que uno mismo envió). La política aquí
     *   solo valida la pertenencia; el filtrado por sender != receiver vive en el repositorio
     *   para evitar un round-trip por mensaje.
     */
    public void validateMarkAsRead(ChatSession session, User receiver) {
        validateParticipantAccess(session, receiver, "Solo los participantes de la sesión pueden marcar mensajes como leídos.");
    }

    /**
     * Verifica que un mensaje pertenece a la sesión esperada. Salvaguarda contra IDs cruzados.
     */
    public void ensureMessageBelongsToSession(ChatMessage message, ChatSession session) {
        if (message == null || message.getChatSession() == null
                || !message.getChatSession().getId().equals(session.getId())) {
            throw new UnauthorizedOperationException("El mensaje no pertenece a la sesión indicada.");
        }
    }

    public boolean isPatient(ChatSession session, User user) {
        if (session == null || user == null) {
            return false;
        }
        Patient patient = session.getPatient();
        return patient != null
                && patient.getUser() != null
                && patient.getUser().getId().equals(user.getId());
    }

    public boolean isPractitioner(ChatSession session, User user) {
        if (session == null || user == null) {
            return false;
        }
        Practitioner practitioner = session.getPractitioner();
        return practitioner != null
                && practitioner.getUser() != null
                && practitioner.getUser().getId().equals(user.getId());
    }

    // Helpers privados

    private void validateParticipantAccess(ChatSession session, User user, String errorMessage) {
        if (session == null || user == null) {
            throw new IllegalArgumentException("La sesión de chat y el usuario no pueden ser nulos.");
        }
        Long userId = user.getId();
        Long patientUserId = session.getPatient().getUser().getId();
        Long practitionerUserId = session.getPractitioner().getUser().getId();
        if (!userId.equals(patientUserId) && !userId.equals(practitionerUserId)) {
            throw new UnauthorizedOperationException(errorMessage);
        }
    }
}
