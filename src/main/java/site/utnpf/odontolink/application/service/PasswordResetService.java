package site.utnpf.odontolink.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IPasswordResetUseCase;
import site.utnpf.odontolink.application.port.out.IEmailSenderPort;
import site.utnpf.odontolink.domain.exception.InvalidPasswordResetTokenException;
import site.utnpf.odontolink.domain.model.PasswordResetToken;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.PasswordResetTokenRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Servicio de aplicación que implementa el flujo de recuperación de contraseña (RF04).
 *
 * Orquesta los siguientes componentes:
 * - {@link UserRepository}: para resolver el usuario titular del email.
 * - {@link PasswordResetTokenRepository}: para emitir, consultar y consumir tokens.
 * - {@link IEmailSenderPort}: para entregar el token al usuario.
 * - {@link PasswordEncoder}: para hashear la nueva contraseña.
 *
 * Decisiones de diseño explicadas:
 * - El token en claro nunca se persiste: la BD guarda únicamente su hash SHA-256.
 *   Esto limita el daño ante un eventual leak: los atacantes recibirían valores
 *   irreversibles que no permiten completar el flujo.
 * - {@code requestPasswordReset} es silenciosa frente a emails inexistentes para
 *   mitigar enumeración de cuentas.
 * - Antes de emitir un token, se invalidan los anteriores aún vigentes del usuario
 *   para impedir que coexistan múltiples tokens válidos asociados a una cuenta.
 */
@Transactional
public class PasswordResetService implements IPasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * Longitud en bytes de la fuente de entropía del token. 32 bytes (256 bits)
     * superan ampliamente los umbrales recomendados por OWASP para tokens
     * efímeros y producen una cadena Base64URL de 43 caracteres.
     */
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final IEmailSenderPort emailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final long tokenTtlMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                IEmailSenderPort emailSender,
                                PasswordEncoder passwordEncoder,
                                long tokenTtlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtlMinutes = tokenTtlMinutes;
        // SecureRandom se mantiene como dependencia interna del servicio porque
        // su construcción es costosa y la clase es thread-safe; reutilizarla
        // evita gastos repetidos de inicialización de entropía.
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Cortar silenciosamente si el email no corresponde a ningún usuario.
        // Loguear a nivel debug para diagnóstico interno sin abrir un canal lateral.
        if (userOpt.isEmpty()) {
            log.debug("Solicitud de reset para un email no registrado; respuesta uniforme aplicada.");
            return;
        }

        User user = userOpt.get();
        Instant now = Instant.now();

        // Política de unicidad temporal: cada nueva solicitud invalida los
        // tokens previos del usuario aún válidos. Evita que un atacante que
        // capture un token antiguo lo combine con uno nuevo no usado.
        tokenRepository.invalidateActiveTokensForUser(user.getId(), now);

        String plainToken = generateSecureToken();
        String tokenHash = hashToken(plainToken);
        Instant expiresAt = now.plus(Duration.ofMinutes(tokenTtlMinutes));

        PasswordResetToken token = new PasswordResetToken(user.getId(), tokenHash, expiresAt);
        tokenRepository.save(token);

        String fullName = buildFullName(user);
        emailSender.sendPasswordResetEmail(user.getEmail(), fullName, plainToken, tokenTtlMinutes);
    }

    @Override
    public void confirmPasswordReset(String plainToken, String newPassword) {
        // El hash se calcula sobre el token presentado por el cliente y se
        // confronta con el almacenado: nunca comparamos valores en claro.
        String tokenHash = hashToken(plainToken);
        Instant now = Instant.now();

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "El token de recuperación es inválido o ya fue utilizado."));

        if (!token.isUsable(now)) {
            throw new InvalidPasswordResetTokenException(
                    "El token de recuperación es inválido o ya fue utilizado.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "El token de recuperación es inválido o ya fue utilizado."));

        // Actualización atómica: marcar el token como consumido y persistir el
        // nuevo hash de contraseña. La anotación @Transactional garantiza que
        // ambos cambios se confirmen juntos o se reviertan ante un fallo.
        user.changePassword(user.getPassword(), passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markAsUsed(now);
        tokenRepository.save(token);

        log.info("Contraseña restablecida correctamente para el usuario id={}.", user.getId());
    }

    /**
     * Genera un token aleatorio de 32 bytes y lo codifica en Base64 URL-safe
     * sin padding para que pueda viajar en un query parameter de un enlace de
     * recuperación sin necesidad de escapado adicional.
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Aplica SHA-256 sobre el token y devuelve el digest en hexadecimal.
     * Se prefiere hash determinístico sobre BCrypt para esta capa porque
     * necesitamos lookup por valor exacto en la BD: BCrypt produce hashes
     * distintos por el salt y rompería la búsqueda.
     */
    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 forma parte del estándar de la JVM; este branch sólo
            // se alcanzaría con una JVM rota, por eso se propaga como error.
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM.", e);
        }
    }

    private String buildFullName(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
