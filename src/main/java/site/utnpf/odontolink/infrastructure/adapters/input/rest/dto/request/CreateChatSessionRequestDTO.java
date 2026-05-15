package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

/**
 * Cuerpo del POST /api/chat/sessions (P4).
 *
 * <p>El servidor resuelve el rol del autor desde el JWT y completa el lado propio del par
 * (paciente o practicante). El frontend manda solo el ID del <b>otro</b> participante,
 * pero por flexibilidad también acepta el propio: si lo manda, debe coincidir con el del
 * token o el servidor responde 403 {@code CHAT_PARTICIPANT_MISMATCH}.
 *
 * @author OdontoLink Team
 */
public class CreateChatSessionRequestDTO {

    /**
     * ID del paciente participante. Cuando el autor es paciente, opcional (se completa del JWT);
     * cuando es practicante, obligatorio.
     */
    private Long patientId;

    /**
     * ID del practicante participante. Cuando el autor es practicante, opcional (se completa
     * del JWT); cuando es paciente, obligatorio.
     */
    private Long practitionerId;

    public CreateChatSessionRequestDTO() {
    }

    public CreateChatSessionRequestDTO(Long patientId, Long practitionerId) {
        this.patientId = patientId;
        this.practitionerId = practitionerId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getPractitionerId() {
        return practitionerId;
    }

    public void setPractitionerId(Long practitionerId) {
        this.practitionerId = practitionerId;
    }
}
