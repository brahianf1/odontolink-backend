package site.utnpf.odontolink.domain.exception;

/**
 * Disparada cuando el {@code currentPassword} provisto al endpoint de cambio
 * de contrasenia autenticado no coincide con el hash almacenado del usuario.
 *
 * <p>Se modela como excepcion separada de
 * {@link AuthenticationFailedException} (que sigue cubriendo el login fallido
 * y devuelve 401) porque el contexto es distinto: aqui el usuario YA esta
 * autenticado correctamente — su token es valido y se acepto la request — y
 * lo que falla es la verificacion adicional de identidad en el payload.
 * Devolver 401 confundiria a los clientes con interceptores que disparan
 * auto-logout ante un 401 generico. Por eso este caso se mapea a 422
 * (Unprocessable Entity) con un {@code error} discriminable que el frontend
 * puede ignorar a proposito en su interceptor de sesion.
 */
public class IncorrectCurrentPasswordException extends RuntimeException {

    public IncorrectCurrentPasswordException(String message) {
        super(message);
    }
}
