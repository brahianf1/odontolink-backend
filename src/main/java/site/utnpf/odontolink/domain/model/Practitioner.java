package site.utnpf.odontolink.domain.model;

import java.util.Set;

/**
 * Contiene solo los datos ESPECÍFICOS del practicante.
 */
public class Practitioner {
    private Long id;

    /** Relación 1-a-1: Un Practicante ES un User */
    private User user;

    // Campos Específicos del Practicante
    private String studentId;     // "Legajo"
    private Integer studyYear;    // "Año Cursado"

    /**
     * Relación N-a-N: Un practicante puede ser supervisado
     * por múltiples docentes (ej. en diferentes cátedras).
     */
    private Set<Supervisor> supervisors;

    /**
     * Relación 1-a-N: El "Catálogo Personal" del practicante.
     * Son los tratamientos que él ha configurado y ofrece.
     */
    private Set<OfferedTreatment> offeredTreatments;

    /**
     * Relación 1-a-N: Un practicante gestiona múltiples casos de atención.
     * Representa "Mis Atenciones".
     */
    private Set<Attention> attentions;
}