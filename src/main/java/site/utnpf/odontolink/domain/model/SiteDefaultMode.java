package site.utnpf.odontolink.domain.model;

/**
 * Modo de color por defecto que aplica el frontend al renderizar la primera
 * carga: claro fijo, oscuro fijo o seguir la preferencia del sistema
 * operativo del usuario.
 *
 * <p>Se persiste como string en BD (no como ordinal) para que sea legible y
 * estable frente a un eventual reordenamiento del enum en el futuro.
 */
public enum SiteDefaultMode {
    LIGHT,
    DARK,
    SYSTEM
}
