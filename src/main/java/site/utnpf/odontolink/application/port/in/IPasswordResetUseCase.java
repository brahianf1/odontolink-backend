package site.utnpf.odontolink.application.port.in;

/**
 * Puerto de entrada (Use Case) para el flujo de recuperación de contraseña (RF04).
 *
 * Modela las dos operaciones del flujo:
 * 1) Solicitud: el usuario informa su email y el sistema emite un token
 *    válido por una ventana corta, que se entrega por correo.
 * 2) Confirmación: el usuario presenta el token y una nueva contraseña;
 *    si el token es válido, se actualiza el hash de la contraseña.
 *
 * El método de solicitud es deliberadamente "silencioso": no retorna
 * información que permita inferir si un email existe o no, para mitigar
 * ataques de enumeración de cuentas.
 */
public interface IPasswordResetUseCase {

    /**
     * Inicia el flujo de recuperación de contraseña para el email dado.
     *
     * Por diseño no expone si el email existe o no; cualquier branch interno
     * que detecte ausencia de usuario debe ser silenciosa para que el
     * comportamiento observable desde fuera sea idéntico en ambos casos.
     */
    void requestPasswordReset(String email);

    /**
     * Confirma el restablecimiento de la contraseña con el token recibido.
     *
     * @throws site.utnpf.odontolink.domain.exception.InvalidPasswordResetTokenException
     *         si el token no existe, está expirado o ya fue consumido.
     */
    void confirmPasswordReset(String plainToken, String newPassword);
}
