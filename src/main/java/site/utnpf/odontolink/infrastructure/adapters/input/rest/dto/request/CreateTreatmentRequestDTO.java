package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación de un tratamiento maestro.
 * Usado por el administrador para agregar tratamientos al catálogo general.
 */
public class CreateTreatmentRequestDTO {

    @NotBlank(message = "El nombre del tratamiento es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String name;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String description;

    @Size(max = 50, message = "El área no puede exceder los 50 caracteres")
    private String area;

    // Constructores
    public CreateTreatmentRequestDTO() {
    }

    public CreateTreatmentRequestDTO(String name, String description, String area) {
        this.name = name;
        this.description = description;
        this.area = area;
    }

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}
