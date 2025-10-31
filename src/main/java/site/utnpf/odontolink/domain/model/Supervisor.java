package site.utnpf.odontolink.domain.model;

import java.util.Set;

/**
 * Contiene solo los datos ESPECÍFICOS del docente.
 */
public class Supervisor {
    private Long id;

    /** Relación 1-a-1: Un Supervisor ES un User */
    private User user;

    // Campos Específicos del Supervisor
    private String specialty; // "Especialidad"

    /**
     * Relación N-a-N: Un supervisor gestiona múltiples practicantes a su cargo.
     */
    private Set<Practitioner> supervisedPractitioners;
}