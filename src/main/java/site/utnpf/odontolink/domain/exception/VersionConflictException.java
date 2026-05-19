package site.utnpf.odontolink.domain.exception;

/**
 * Senaliza que un PUT/UPDATE llego con un numero de version (header
 * {@code If-Match}) que no coincide con el actual en BD: significa que otro
 * actor modifico el recurso entre el GET del cliente y este PUT.
 *
 * <p>Se mapea a HTTP 409 con {@code errorCode=VERSION_CONFLICT} para que el
 * frontend pueda recargar y reintentar sin tener que parsear el mensaje
 * humano. Lleva la {@code currentVersion} para que el cliente sepa contra que
 * version sincronizar.
 *
 * <p>Es la primera vez que el codebase introduce optimistic locking expuesto
 * a HTTP (greenfield). La razon de existir como excepcion de dominio (y no
 * de infraestructura) es que el invariante "no podes pisar cambios mas
 * nuevos" es una regla de negocio, no un detalle de transporte.
 */
public class VersionConflictException extends DomainException {

    private final int currentVersion;

    public VersionConflictException(int currentVersion) {
        super("La version del recurso fue modificada por otro actor; recarga y reintenta.",
                "VERSION_CONFLICT");
        this.currentVersion = currentVersion;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }
}
