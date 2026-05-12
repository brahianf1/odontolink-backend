package site.utnpf.odontolink.infrastructure.adapters.output.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.utnpf.odontolink.application.port.out.IEmailSenderPort;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Implementación "mock" del puerto {@link IEmailSenderPort}.
 *
 * En lugar de enviar un correo real, imprime al log el contenido del mensaje
 * en un formato visualmente claro para que sea fácil de leer durante el
 * desarrollo. Esta implementación permite avanzar con el flujo de RF04 sin
 * depender de un proveedor SMTP configurado; el día que se introduzca uno,
 * basta con sustituir este bean por un adaptador SMTP/SES/SendGrid sin tocar
 * el caso de uso ni el dominio.
 */
@Component
public class ConsoleEmailSenderAdapter implements IEmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSenderAdapter.class);

    /**
     * URL base del frontend a la que se anexa el token, sólo para fines de
     * visualización en el log. El backend no depende funcionalmente de esta
     * propiedad: si el frontend cambia, el adaptador SMTP futuro tomará el
     * valor correcto sin alterar la lógica del flujo.
     */
    @Value("${odontolink.password-reset.frontend-url:http://localhost:3000/reset-password}")
    private String frontendResetUrl;

    @Override
    public void sendPasswordResetEmail(String recipientEmail,
                                       String recipientName,
                                       String plainToken,
                                       long expirationMinutes) {

        String safeToken = URLEncoder.encode(plainToken, StandardCharsets.UTF_8);
        String resetLink = frontendResetUrl + "?token=" + safeToken;

        // El uso de log.info y un formato encuadrado facilita la lectura en la
        // consola del desarrollador. Importante: este log se justifica
        // únicamente porque estamos en una implementación mock; un adaptador
        // real jamás debe imprimir el token en claro.
        log.info(
                """

                ============================================================
                  [OdontoLink] CORREO DE RECUPERACIÓN DE CONTRASEÑA (MOCK)
                ============================================================
                  Para     : {} <{}>
                  Asunto   : Restablecé tu contraseña de OdontoLink
                  Vigencia : {} minutos
                ------------------------------------------------------------
                  Hola {},

                  Recibimos una solicitud para restablecer la contraseña de
                  tu cuenta en OdontoLink. Si fuiste vos, hacé clic en el
                  siguiente enlace para crear una nueva contraseña:

                  {}

                  También podés copiar y pegar el siguiente token en la
                  aplicación si el enlace no funciona:

                  TOKEN: {}

                  Este token vencerá en {} minutos. Si no fuiste vos quien
                  solicitó este cambio, podés ignorar este mensaje: tu
                  contraseña seguirá siendo la misma.
                ============================================================
                """,
                recipientName,
                recipientEmail,
                expirationMinutes,
                recipientName,
                resetLink,
                plainToken,
                expirationMinutes
        );
    }
}
