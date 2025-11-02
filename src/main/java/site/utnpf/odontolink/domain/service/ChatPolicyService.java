package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.User;

/**
 * Servicio de Dominio que implementa las políticas y reglas de negocio complejas
 * relacionadas con el sistema de chat interno entre paciente y practicante.
 *
 * Este servicio actúa como un "Rulebook" para operaciones de chat que requieren
 * validaciones de seguridad y pertenencia.
 *
 * Implementa las reglas de negocio de:
 * - RF26: Chat interno paciente-practicante
 * - RF27: Restricción de chat sin relación previa
 *
 * Responsabilidades:
 * 1. Validar que el usuario es un participante legítimo de la sesión de chat
 * 2. Validar que el remitente de un mensaje pertenece a la sesión
 * 3. Aplicar las reglas de privacidad y seguridad del chat
 *
 * @author OdontoLink Team
 */
public class ChatPolicyService {

    /**
     * Valida que un usuario que intenta enviar un mensaje es un participante legítimo
     * de la sesión de chat. Implementa RF26 - Seguridad del chat.
     *
     * Reglas de negocio:
     * - Solo el paciente o el practicante de la sesión pueden enviar mensajes
     * - Se valida comparando el User del sender con los User de los participantes
     *
     * @param session La sesión de chat
     * @param sender El usuario que intenta enviar el mensaje
     * @throws UnauthorizedOperationException si el usuario no pertenece a la sesión
     */
    public void validateMessageSend(ChatSession session, User sender) {
        if (session == null || sender == null) {
            throw new IllegalArgumentException("La sesión de chat y el remitente no pueden ser nulos.");
        }

        Long senderUserId = sender.getId();
        Long patientUserId = session.getPatient().getUser().getId();
        Long practitionerUserId = session.getPractitioner().getUser().getId();

        if (!senderUserId.equals(patientUserId) && !senderUserId.equals(practitionerUserId)) {
            throw new UnauthorizedOperationException("El usuario no pertenece a esta sesión de chat.");
        }
    }

    /**
     * Valida que un usuario puede acceder a los mensajes de una sesión de chat.
     * Reutiliza la lógica de validateMessageSend ya que los permisos son los mismos.
     *
     * Reglas de negocio:
     * - Solo el paciente o el practicante de la sesión pueden ver los mensajes
     *
     * @param session La sesión de chat
     * @param user El usuario que intenta acceder a los mensajes
     * @throws UnauthorizedOperationException si el usuario no tiene permisos
     */
    public void validateMessageAccess(ChatSession session, User user) {
        // Reutilizamos la validación de envío ya que los permisos son idénticos
        validateMessageSend(session, user);
    }

    /**
     * Verifica si un usuario es el paciente de una sesión de chat.
     *
     * @param session La sesión de chat
     * @param user El usuario a verificar
     * @return true si el usuario es el paciente de la sesión
     */
    public boolean isPatient(ChatSession session, User user) {
        if (session == null || user == null) {
            return false;
        }

        Patient patient = session.getPatient();
        return patient != null
                && patient.getUser() != null
                && patient.getUser().getId().equals(user.getId());
    }

    /**
     * Verifica si un usuario es el practicante de una sesión de chat.
     *
     * @param session La sesión de chat
     * @param user El usuario a verificar
     * @return true si el usuario es el practicante de la sesión
     */
    public boolean isPractitioner(ChatSession session, User user) {
        if (session == null || user == null) {
            return false;
        }

        Practitioner practitioner = session.getPractitioner();
        return practitioner != null
                && practitioner.getUser() != null
                && practitioner.getUser().getId().equals(user.getId());
    }
}
