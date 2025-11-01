package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

/**
 * DTO de respuesta para un tratamiento del catálogo maestro.
 */
public class TreatmentResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String area;

    // Constructores
    public TreatmentResponseDTO() {
    }

    public TreatmentResponseDTO(Long id, String name, String description, String area) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.area = area;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
