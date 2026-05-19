package site.utnpf.odontolink.domain.exception;

/**
 * Senaliza que se intento soft-deletar un custom theme que esta actualmente
 * referenciado como {@code themeVariantId} en la configuracion singleton de
 * appearance.
 *
 * <p>Borrar lo dejaria al sitio apuntando a un slug colgado y los usuarios
 * cargarian un theme inexistente. El admin debe primero cambiar la appearance
 * a otro theme (built-in o custom) y recien despues borrar.
 *
 * <p>Se mapea a HTTP 409 con {@code errorCode=THEME_IN_USE}.
 */
public class ThemeInUseException extends DomainException {

    private final String slug;

    public ThemeInUseException(String slug) {
        super("El theme '" + slug + "' esta en uso como appearance activa; cambia la "
                + "appearance a otro theme antes de eliminarlo.",
                "THEME_IN_USE");
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }
}
