package site.utnpf.odontolink.domain.exception;

/**
 * Excepción de dominio lanzada cuando no se encuentra un recurso solicitado.
 */
public class ResourceNotFoundException extends DomainException {

    private final String resourceType;
    private final String searchField;
    private final String searchValue;

    public ResourceNotFoundException(String resourceType, String searchField, String searchValue) {
        super(String.format("%s con %s '%s' no encontrado", resourceType, searchField, searchValue));
        this.resourceType = resourceType;
        this.searchField = searchField;
        this.searchValue = searchValue;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getSearchField() {
        return searchField;
    }

    public String getSearchValue() {
        return searchValue;
    }
}
