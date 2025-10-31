package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta crear un recurso
 * que ya existe (email duplicado, DNI duplicado, etc.).
 */
public class DuplicateResourceException extends DomainException {

    private final String resourceType;
    private final String field;
    private final String value;

    public DuplicateResourceException(String resourceType, String field, String value) {
        super(String.format("El %s con %s '%s' ya existe", resourceType, field, value));
        this.resourceType = resourceType;
        this.field = field;
        this.value = value;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
