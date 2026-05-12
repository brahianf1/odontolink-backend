package site.utnpf.odontolink.application.port.out;

/**
 * Puerto de salida (Output Port) para el envío de correos electrónicos.
 *
 * Desacopla la capa de aplicación del proveedor de mensajería concreto:
 * en este momento un adaptador "console" que imprime el mensaje al log,
 * en el futuro un adaptador SMTP, SendGrid, SES, etc. Inyectar la abstracción
 * permite cambiar la implementación sin tocar el caso de uso.
 */
public interface IEmailSenderPort {

    /**
     * Envía el correo de recuperación de contraseña.
     *
     * @param recipientEmail   dirección de correo del destinatario.
     * @param recipientName    nombre completo del destinatario (para personalización).
     * @param plainToken       token en claro a entregar al usuario; el sistema
     *                         conserva sólo su hash, por lo que este valor
     *                         únicamente existe en memoria al momento de emisión.
     * @param expirationMinutes minutos hasta la expiración del token, comunicados
     *                          al usuario para que sepa la ventana disponible.
     */
    void sendPasswordResetEmail(String recipientEmail,
                                String recipientName,
                                String plainToken,
                                long expirationMinutes);
}
