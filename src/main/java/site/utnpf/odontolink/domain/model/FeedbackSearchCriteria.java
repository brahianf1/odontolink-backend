package site.utnpf.odontolink.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/**
 * Value Object inmutable que encapsula los criterios de búsqueda del Panel
 * Docente de Supervisión de Feedback (RF25).
 *
 * Decisión arquitectónica: vive en el Dominio porque expresa una intención
 * de negocio analítica (qué subconjunto del universo de Feedback observar)
 * y no debe acoplar la capa de aplicación a Spring/JPA. La traducción a
 * predicados SQL ocurre en el adaptador de persistencia mediante
 * Specifications, en línea con el patrón ya establecido en RF09.
 *
 * Todos los filtros visibles para el cliente son OPCIONALES y combinables
 * con AND lógico:
 *  - practitionerId: limita el universo a un practicante puntual.
 *  - patientId: limita el universo a un paciente puntual.
 *  - treatmentId: limita el universo a un tipo de tratamiento.
 *  - startDate / endDate: ventana inclusiva sobre el campo createdAt del
 *    feedback. Si sólo viene una de las dos, se aplica como cota abierta.
 *
 * Filtro INVISIBLE (el "cerco" docente-alumno - RF25/RF40):
 *  - allowedPractitionerIds: lista cerrada de practicantes que el
 *    supervisor autenticado tiene vinculados. El adaptador SIEMPRE inyecta
 *    un {@code practitioner.id IN (...)} con esta lista para que un docente
 *    JAMÁS pueda ver feedback de un alumno ajeno, sin importar qué filtros
 *    intente combinar el frontend. Si el conjunto está vacío, el resultado
 *    debe ser un universo vacío (no se devuelve nada).
 */
public final class FeedbackSearchCriteria {

    private final Long practitionerId;
    private final Long patientId;
    private final Long treatmentId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Cerco silencioso. Inmutable. Nunca null tras el constructor: se
     * normaliza a un conjunto vacío para que el caller no tenga que
     * defender contra NPE y el adaptador pueda generar un predicado
     * "nunca matchea" si está vacío (resultado vacío seguro por defecto).
     */
    private final Set<Long> allowedPractitionerIds;

    public FeedbackSearchCriteria(Long practitionerId,
                                  Long patientId,
                                  Long treatmentId,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  Set<Long> allowedPractitionerIds) {
        this.practitionerId = practitionerId;
        this.patientId = patientId;
        this.treatmentId = treatmentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.allowedPractitionerIds = allowedPractitionerIds == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(allowedPractitionerIds);
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

    public Set<Long> getAllowedPractitionerIds() {
        return allowedPractitionerIds;
    }

    public boolean hasPractitionerId() {
        return practitionerId != null;
    }

    public boolean hasPatientId() {
        return patientId != null;
    }

    public boolean hasTreatmentId() {
        return treatmentId != null;
    }

    public boolean hasStartDate() {
        return startDate != null;
    }

    public boolean hasEndDate() {
        return endDate != null;
    }

    /**
     * El cerco docente-alumno está siempre presente (puede ser vacío para
     * forzar resultado vacío). Su semántica no es "filtro opcional" sino
     * "perímetro de seguridad obligatorio".
     */
    public boolean hasAllowedPractitionersScope() {
        return true;
    }
}
