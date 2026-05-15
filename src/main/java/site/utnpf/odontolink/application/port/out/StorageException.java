package site.utnpf.odontolink.application.port.out;

/**
 * Excepcion unchecked que envuelve cualquier falla al interactuar con el
 * object storage. Vive en el paquete del puerto para que los servicios de
 * aplicacion la conozcan sin acoplarse al SDK de un proveedor concreto.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
