package site.utnpf.odontolink.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Value Object que representa una página de resultados devuelta por un
 * puerto de salida, sin acoplar el Dominio a {@code org.springframework.data.domain.Page}.
 *
 * Encapsula el contenido + metadatos de paginación. Es inmutable (la lista
 * recibida se envuelve con {@code unmodifiableList} para evitar mutación
 * accidental por el consumidor).
 *
 * @param <T> Tipo del contenido (típicamente un objeto de dominio)
 */
public final class PageResult<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PageResult(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content != null ? Collections.unmodifiableList(content) : Collections.emptyList();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
