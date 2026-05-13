package site.utnpf.odontolink.application.port.in.dto;

import java.time.LocalDate;

/**
 * Query "limpia" que el controlador entrega al caso de uso del Panel
 * Docente de Feedback (RF25).
 *
 * Por qué un DTO de aplicación en vez de pasar
 * {@code FeedbackSearchCriteria} directo:
 *  - El caso de uso TODAVÍA NO conoce el cerco docente-alumno
 *    (allowedPractitionerIds). Ese cerco lo arma el servicio de aplicación
 *    a partir del usuario autenticado, así que el controlador NUNCA puede
 *    enviarlo. Si expusiéramos {@code FeedbackSearchCriteria} en el puerto,
 *    el controlador podría rellenar (o no rellenar) el cerco a su antojo,
 *    diluyendo la garantía de seguridad.
 *  - Mantiene el puerto desacoplado del Value Object de Dominio: el contrato
 *    de aplicación trabaja con primitivos del cliente HTTP (LocalDate / Long).
 *
 * Todos los campos son OPCIONALES y combinables (AND lógico) — los
 * defaults del cliente HTTP los traducimos en null.
 */
public final class SupervisorFeedbackDashboardQuery {

    private final Long practitionerId;
    private final Long patientId;
    private final Long treatmentId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public SupervisorFeedbackDashboardQuery(Long practitionerId,
                                            Long patientId,
                                            Long treatmentId,
                                            LocalDate startDate,
                                            LocalDate endDate) {
        this.practitionerId = practitionerId;
        this.patientId = patientId;
        this.treatmentId = treatmentId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getPractitionerId() {
        return practitionerId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
