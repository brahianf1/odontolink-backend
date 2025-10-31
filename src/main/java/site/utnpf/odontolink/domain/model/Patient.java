package site.utnpf.odontolink.domain.model;

import java.util.Set;

/**
 * Contiene solo los datos ESPECÍFICOS del paciente.
 * Se vincula 1-a-1 con la entidad User.
 */
public class Patient {
    private Long id;

    /** Relación 1-a-1: Un Paciente ES un User */
    private User user;

    // Campos Específicos del Paciente
    private String healthInsurance; // "Obra Social"
    private String bloodType;       // "Tipo de Sangre"

    /** Relación 1-a-N: Un paciente puede tener múltiples casos de atención */
    private Set<Attention> attentions;
}