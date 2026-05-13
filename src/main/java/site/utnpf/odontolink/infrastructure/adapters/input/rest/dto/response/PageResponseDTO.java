package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.PageResult;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DTO genérico para respuestas paginadas (RF09 — soporte UX/Performance).
 *
 * Centraliza el formato de paginación expuesto a la API para que el
 * frontend reciba siempre la misma forma: contenido + metadata necesaria
 * (totalElements, totalPages, página/size actuales, flags hasNext/hasPrev).
 *
 * Diseño deliberado: NO se expone {@code Page} de Spring por dos razones:
 * 1) Acoplaría el contrato HTTP al modelo interno de Spring Data.
 * 2) La serialización de {@code Page} es notoria por incluir campos que
 *    rompen contratos cuando se actualiza la versión del framework.
 *
 * @param <T> Tipo del contenido (DTO de respuesta)
 */
@Schema(description = "Estructura genérica para respuestas paginadas")
public class PageResponseDTO<T> {

    @Schema(description = "Elementos de la página actual")
    private List<T> content;

    @Schema(description = "Índice 0-based de la página actual", example = "0")
    private int page;

    @Schema(description = "Tamaño solicitado de página", example = "20")
    private int size;

    @Schema(description = "Cantidad total de elementos sin paginar", example = "137")
    private long totalElements;

    @Schema(description = "Cantidad total de páginas con el size actual", example = "7")
    private int totalPages;

    @Schema(description = "Existe una página siguiente", example = "true")
    private boolean hasNext;

    @Schema(description = "Existe una página previa", example = "false")
    private boolean hasPrevious;

    public PageResponseDTO() {
    }

    /**
     * Construye un {@link PageResponseDTO} a partir de un {@link PageResult}
     * del dominio aplicando un mapper de elemento → DTO.
     *
     * Mantiene la conversión Dominio → DTO en un único lugar (este factory)
     * para que los controllers no se llenen de boilerplate.
     */
    public static <D, R> PageResponseDTO<R> of(PageResult<D> pageResult, Function<D, R> mapper) {
        PageResponseDTO<R> dto = new PageResponseDTO<>();
        dto.content = pageResult.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        dto.page = pageResult.getPage();
        dto.size = pageResult.getSize();
        dto.totalElements = pageResult.getTotalElements();
        dto.totalPages = pageResult.getTotalPages();
        dto.hasNext = pageResult.hasNext();
        dto.hasPrevious = pageResult.hasPrevious();
        return dto;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
