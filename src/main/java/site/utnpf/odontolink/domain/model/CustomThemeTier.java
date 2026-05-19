package site.utnpf.odontolink.domain.model;

/**
 * Clasificacion de un custom theme. Lo usa el frontend para diferenciar en la
 * UI entre paletas pulidas listas para clientes ({@code OFFICIAL}) y paletas
 * en evaluacion o draft cosmetico ({@code EXPERIMENTAL}).
 *
 * <p>No tiene impacto en autorizacion ni en logica: es metadata para el
 * catalogo. Se persiste como string para no atarse al orden del enum.
 */
public enum CustomThemeTier {
    OFFICIAL,
    EXPERIMENTAL
}
