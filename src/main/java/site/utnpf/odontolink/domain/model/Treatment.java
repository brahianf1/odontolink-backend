package site.utnpf.odontolink.domain.model;

/**
 * Representa el catálogo general de tratamientos que la institución ofrece.
 * Ej: "Limpieza Dental", "Endodoncia". Es una entidad maestra.
 */
public class Treatment {
    private Long id;
    private String name;
    private String description;
    private String area; // ej: "General", "Ortodoncia"
}