package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para un tratamiento del catálogo maestro.
 */
@Schema(description = "Tratamiento del catálogo maestro institucional, referenciado por las ofertas " +
        "del catálogo personal de cada practicante.")
public class TreatmentResponseDTO {

    @Schema(description = "Identificador único del tratamiento maestro.", example = "1")
    private Long id;

    @Schema(description = "Nombre del tratamiento.", example = "Limpieza completa")
    private String name;

    @Schema(description = "Descripción del tratamiento mostrada al paciente.",
            example = "Eliminación de placa y sarro total")
    private String description;

    @Schema(description = "Área odontológica a la que pertenece el tratamiento.",
            example = "ORTODONCIA")
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
