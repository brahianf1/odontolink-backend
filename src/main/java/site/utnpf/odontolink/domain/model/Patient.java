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

    // Constructores
    public Patient() {
    }

    public Patient(User user, String healthInsurance, String bloodType) {
        this.user = user;
        this.healthInsurance = healthInsurance;
        this.bloodType = bloodType;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHealthInsurance() {
        return healthInsurance;
    }

    public void setHealthInsurance(String healthInsurance) {
        this.healthInsurance = healthInsurance;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Set<Attention> getAttentions() {
        return attentions;
    }

    public void setAttentions(Set<Attention> attentions) {
        this.attentions = attentions;
    }
}