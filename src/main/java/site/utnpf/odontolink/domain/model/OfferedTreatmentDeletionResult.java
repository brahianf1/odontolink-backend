package site.utnpf.odontolink.domain.model;

/**
 * Resultado de la operación "eliminar oferta del catálogo" (RF16).
 *
 * Encapsula la decisión que toma el Dominio según los compromisos vivos:
 * - {@link Outcome#SOFT_DELETED}: existen turnos SCHEDULED futuros o
 *   Atenciones IN_PROGRESS. La oferta se marca como inactiva para que
 *   desaparezca del catálogo público preservando la cadena referencial
 *   de turnos/atenciones ya otorgados.
 * - {@link Outcome#HARD_DELETED}: no hay compromisos vivos ni
 *   atenciones históricas referenciando esta oferta (par practitioner+
 *   treatment); se elimina físicamente.
 *
 * El servicio de aplicación devuelve esta estructura para que el adaptador
 * REST traduzca la decisión al cliente con un mensaje accionable (mejor UX
 * que un genérico 204 No Content que oculta la consecuencia del clic).
 */
public final class OfferedTreatmentDeletionResult {

    public enum Outcome {
        /** La oferta se desactivó preservando integridad referencial. */
        SOFT_DELETED,
        /** La oferta se eliminó físicamente del catálogo. */
        HARD_DELETED
    }

    private final Outcome outcome;
    private final String reason;

    public OfferedTreatmentDeletionResult(Outcome outcome, String reason) {
        this.outcome = outcome;
        this.reason = reason;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSoftDeleted() {
        return outcome == Outcome.SOFT_DELETED;
    }
}
