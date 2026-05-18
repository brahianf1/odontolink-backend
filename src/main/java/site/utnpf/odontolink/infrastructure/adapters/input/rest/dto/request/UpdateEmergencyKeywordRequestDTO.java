package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateEmergencyKeywordRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String term;

    private boolean active;

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
