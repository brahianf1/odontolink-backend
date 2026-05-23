package site.utnpf.odontolink.domain.model;

/**
 * Códigos canónicos de los {@link FeedbackCriterion} sembrados por el
 * bootstrapper. Compartidos por todo el dominio para evitar strings
 * sueltos en bootstrapper, mapper de aggregates legacy y tests.
 *
 * <p>El frontend consume los códigos vía el endpoint de catálogo. No los
 * hardcodea en código — esta clase sólo asegura coherencia en el backend.
 */
public final class FeedbackCriterionCodes {

    public static final String PUNCTUALITY = "PUNCTUALITY";
    public static final String CARE_QUALITY = "CARE_QUALITY";
    public static final String COMMUNICATION_CLARITY = "COMMUNICATION_CLARITY";
    public static final String GENERAL_SATISFACTION = "GENERAL_SATISFACTION";
    public static final String PATIENT_BEHAVIOR = "PATIENT_BEHAVIOR";

    private FeedbackCriterionCodes() {
    }
}
