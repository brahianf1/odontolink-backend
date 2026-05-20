package site.utnpf.odontolink.domain.model;

/**
 * Discrimina las dos direcciones del feedback bidireccional del sistema.
 *
 * <p>Una {@link Attention} finalizada admite hasta dos {@link Feedback}: uno
 * que envía el paciente calificando al practicante (RF21) y otro que envía
 * el practicante calificando al paciente (RF22). El campo
 * {@link Feedback#getSubmittedBy()} distingue quién emitió cada uno; este
 * enum nombra esa distinción para que los criterios analíticos del Panel
 * Docente (RF25) y cualquier futuro consumidor no tengan que reimplementar
 * el chequeo "¿este feedback es del paciente o del practicante?".
 *
 * <p>Decisión arquitectónica: vive en el Dominio porque expresa una regla
 * de negocio (el feedback bidireccional como concepto) y no un detalle
 * técnico de persistencia o de transporte HTTP.
 */
public enum FeedbackDirection {

    /**
     * Feedback emitido por el paciente sobre el practicante. Es la métrica
     * relevante para evaluar el desempeño del practicante en el Panel
     * Docente: refleja la experiencia del paciente con la atención clínica
     * y las soft skills del estudiante.
     */
    PATIENT_TO_PRACTITIONER,

    /**
     * Feedback emitido por el practicante sobre el paciente. Es información
     * útil para el practicante pero NO debe mezclarse con la métrica de
     * desempeño del estudiante: un practicante que atiende a un paciente
     * difícil puede legítimamente bajarle la nota sin que eso refleje su
     * propio desempeño.
     */
    PRACTITIONER_TO_PATIENT;

    /**
     * Resuelve la dirección de un feedback inspeccionando quién lo envió.
     *
     * @return la dirección si el {@code submittedBy} coincide con el
     *         paciente o el practicante de la atención asociada,
     *         {@code null} si el feedback es inconsistente (no debería
     *         ocurrir: {@link FeedbackPolicyService} lo impide en la
     *         escritura).
     */
    public static FeedbackDirection of(Feedback feedback) {
        if (feedback == null || feedback.getSubmittedBy() == null || feedback.getAttention() == null) {
            return null;
        }
        Attention attention = feedback.getAttention();
        Long submittedById = feedback.getSubmittedBy().getId();

        if (attention.getPatient() != null
                && attention.getPatient().getUser() != null
                && submittedById.equals(attention.getPatient().getUser().getId())) {
            return PATIENT_TO_PRACTITIONER;
        }
        if (attention.getPractitioner() != null
                && attention.getPractitioner().getUser() != null
                && submittedById.equals(attention.getPractitioner().getUser().getId())) {
            return PRACTITIONER_TO_PATIENT;
        }
        return null;
    }
}
