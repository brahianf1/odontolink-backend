package site.utnpf.odontolink.domain.model;

import java.util.Set;

/**
 * Representa la OFERTA de un tratamiento específico por un practicante.
 * Esta es la entidad clave del "Catálogo Personal".
 */
public class OfferedTreatment {
    private Long id;

    /** Relación N-a-1: El practicante que ofrece esto */
    private Practitioner practitioner;

    /** Relación N-a-1: El tratamiento maestro que se ofrece */
    private Treatment treatment;

    private String requirements; // "Requisitos específicos"

    /**
     * Relación 1-a-N: Las franjas horarias estructuradas
     * para esta oferta (disponibilidad).
     */
    private Set<AvailabilitySlot> availabilitySlots;
}