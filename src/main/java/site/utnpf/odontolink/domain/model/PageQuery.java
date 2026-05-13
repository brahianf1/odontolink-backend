package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

/**
 * Value Object inmutable que representa una solicitud de paginación.
 *
 * Vive en el Dominio para evitar acoplar la capa de aplicación a
 * {@code org.springframework.data.domain.Pageable}. La traducción a
 * Pageable ocurre en el adaptador de persistencia.
 *
 * Convención: page es 0-based (la primera página es 0) — la misma
 * convención que Spring Data, para reducir la fricción de conversión.
 *
 * El campo {@code sortBy} se acepta como string libre pero el adaptador
 * SOLO acepta valores de un allowlist para evitar inyección por SQL
 * (ver OfferedTreatmentPersistenceAdapter#toSpringSort).
 */
public final class PageQuery {

    /** Máximo de elementos por página, para proteger memoria del backend. */
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final int page;
    private final int size;
    private final String sortBy;
    private final SortDirection sortDirection;

    public PageQuery(int page, int size, String sortBy, SortDirection sortDirection) {
        if (page < 0) {
            throw new InvalidBusinessRuleException("El número de página no puede ser negativo.");
        }
        if (size <= 0) {
            throw new InvalidBusinessRuleException("El tamaño de página debe ser mayor a cero.");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new InvalidBusinessRuleException(
                    "El tamaño de página no puede exceder " + MAX_PAGE_SIZE + " elementos."
            );
        }
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection != null ? sortDirection : SortDirection.ASC;
    }

    /** Constructor de conveniencia con parámetros saneables desde el adaptador REST. */
    public static PageQuery of(Integer page, Integer size, String sortBy, String sortDirection) {
        int effectivePage = (page == null || page < 0) ? 0 : page;
        int effectiveSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        SortDirection direction = SortDirection.fromString(sortDirection);
        return new PageQuery(effectivePage, effectiveSize, sortBy, direction);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public boolean hasSort() {
        return sortBy != null && !sortBy.trim().isEmpty();
    }

    public enum SortDirection {
        ASC, DESC;

        public static SortDirection fromString(String value) {
            if (value == null) {
                return ASC;
            }
            return "DESC".equalsIgnoreCase(value.trim()) ? DESC : ASC;
        }
    }
}
