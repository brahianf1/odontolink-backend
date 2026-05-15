package site.utnpf.odontolink.infrastructure.adapters.input.rest.error;

/**
 * Catálogo de {@code errorCode}s estables del subsistema de chat.
 *
 * <p>Estos códigos viajan en el body de las respuestas 4xx para que el frontend
 * pueda ramificar UX (mostrar banner de "te bloquearon" vs redirigir al inbox por
 * "no perteneces a la sesión") sin depender del mensaje humano, que es i18n y
 * está sujeto a cambios.
 *
 * <p>Convención: nombres en SCREAMING_SNAKE con prefijo {@code CHAT_}.
 * No se eliminan códigos publicados sin un proceso de deprecación visible.
 */
public final class ChatErrorCodes {

    private ChatErrorCodes() {
    }

    // --- 403 Forbidden ----------------------------------------------------

    /**
     * El usuario es participante de la sesión pero está bloqueado: el otro lado
     * lo silenció. El frontend debería mostrar un banner explicativo y deshabilitar
     * la composición de mensajes, pero conservar la posibilidad de leer historial.
     */
    public static final String CHAT_BLOCKED = "CHAT_BLOCKED";

    /**
     * El usuario no pertenece a la sesión (ni es paciente ni practicante de ella).
     * El frontend debería redirigir al inbox o mostrar un error de "sesión inválida"
     * — nunca debió poder llegar a este recurso.
     */
    public static final String CHAT_NOT_PARTICIPANT = "CHAT_NOT_PARTICIPANT";

    /**
     * Solo el practicante de la sesión puede bloquear/desbloquear (RF28). Distinto
     * de {@link #CHAT_NOT_PARTICIPANT} porque el usuario sí pertenece a la sesión,
     * pero no tiene la autoridad clínica requerida.
     */
    public static final String CHAT_NOT_PRACTITIONER_OF_SESSION = "CHAT_NOT_PRACTITIONER_OF_SESSION";

    // --- 422 Unprocessable Entity -----------------------------------------

    /**
     * Se intentó bloquear una sesión que ya estaba bloqueada. Idempotencia explícita:
     * no asumimos que el caller quería re-pisar, lanzamos para que corrija el flujo.
     */
    public static final String CHAT_ALREADY_BLOCKED = "CHAT_ALREADY_BLOCKED";

    /**
     * Se intentó desbloquear una sesión que no estaba bloqueada.
     */
    public static final String CHAT_NOT_BLOCKED = "CHAT_NOT_BLOCKED";

    /**
     * Se intentó crear/abrir una sesión entre un paciente y un practicante que no
     * tienen relación clínica previa (RF27). La sesión solo puede materializarse
     * cuando hubo al menos un appointment entre ambos.
     */
    public static final String CHAT_NO_PRIOR_RELATIONSHIP = "CHAT_NO_PRIOR_RELATIONSHIP";

    /**
     * El rol autenticado no coincide con el participante que está intentando crear:
     * un paciente que mandó un patientId distinto al suyo, o un practicante con
     * practitionerId distinto al suyo. Indica intento de impersonación.
     */
    public static final String CHAT_PARTICIPANT_MISMATCH = "CHAT_PARTICIPANT_MISMATCH";
}
