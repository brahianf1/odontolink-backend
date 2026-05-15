package site.utnpf.odontolink.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import io.github.bucket4j.Bucket;
import site.utnpf.odontolink.application.port.in.IProfileUseCase;
import site.utnpf.odontolink.application.port.in.UpdateProfileCommand;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.exception.IncorrectCurrentPasswordException;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.RateLimitExceededException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.AuthResult;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;
import site.utnpf.odontolink.infrastructure.config.ratelimit.RateLimitRegistry;

import java.time.Instant;

/**
 * Servicio de aplicación que implementa el caso de uso de autoservicio del
 * perfil del usuario (RF06).
 *
 * Decisiones de diseño:
 * <ul>
 *   <li>El identificador del usuario llega siempre desde el contexto de
 *       seguridad (JWT), nunca desde la URL ni el cuerpo: el controlador es
 *       responsable de extraerlo vía {@code AuthenticationFacade}. Así
 *       quedamos blindados ante IDOR sin acoplar la capa de aplicación a
 *       Spring Security.</li>
 *   <li>La unicidad del email se valida con
 *       {@link UserRepository#existsByEmailAndIdNot(String, Long)} para no
 *       acusar de duplicado al propio usuario cuando reenvía su mismo
 *       email.</li>
 *   <li>El cambio de contraseña verifica la contraseña actual usando el
 *       {@link PasswordEncoder} de Spring Security. Si la verificación
 *       falla lanzamos {@link AuthenticationFailedException} con un mensaje
 *       genérico para no leak-ear información de side-channel.</li>
 *   <li>Toda escritura es transaccional para garantizar atomicidad entre
 *       la verificación de unicidad y la persistencia.</li>
 * </ul>
 */
@Transactional
public class ProfileService implements IProfileUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ITokenProvider tokenProvider;
    private final RateLimitRegistry rateLimitRegistry;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          ITokenProvider tokenProvider,
                          RateLimitRegistry rateLimitRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.rateLimitRegistry = rateLimitRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public User getMyProfile(Long userId) {
        return loadAuthenticatedUser(userId);
    }

    @Override
    public User updateMyProfile(Long userId, UpdateProfileCommand command) {
        User user = loadAuthenticatedUser(userId);

        // Validación de unicidad del email sólo si realmente cambia. Comparamos
        // contra el email actual normalizado para evitar disparar la consulta
        // SQL cuando el frontend reenvía el mismo valor que ya tenía el usuario.
        String requestedEmail = command.getEmail();
        if (requestedEmail != null && !requestedEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(requestedEmail, userId)) {
                throw new DuplicateResourceException("Usuario", "email", requestedEmail);
            }
        }

        // Campos requeridos: siempre vienen en el payload (validados por @NotBlank).
        user.updateSelfProfile(
                requestedEmail,
                command.getFirstName(),
                command.getLastName()
        );

        // Campos opcionales con semántica PATCH: ifPresent dispara sólo si el
        // wrapper viene definido en el JSON, preservando el valor previo cuando
        // el cliente omite el campo. Un valor null dentro del wrapper limpia
        // el campo (lo aplica el mapper trasladando "" a null).
        command.getPhone().ifPresent(user::setPhone);
        command.getBirthDate().ifPresent(user::setBirthDate);
        command.getAddress().ifPresent(user::setAddress);
        command.getProfilePictureUrl().ifPresent(user::setProfilePictureUrl);

        return userRepository.save(user);
    }

    @Override
    public AuthResult changeMyPassword(Long userId, String currentPassword, String newPassword) {
        // La validación de presencia ya la garantiza el DTO con @NotBlank. Aquí
        // sumamos la regla de negocio "la nueva contraseña no puede ser igual
        // a la anterior" para preservar la utilidad de una rotación.
        if (newPassword.equals(currentPassword)) {
            throw new InvalidBusinessRuleException(
                    "La nueva contraseña debe ser distinta de la actual.");
        }

        // Rate-limit por fallos consecutivos: si el bucket esta vacio, el
        // usuario ya consumio sus intentos en la ventana actual y rechazamos
        // sin consultar siquiera el hash de contrasenia.
        Bucket failBucket = rateLimitRegistry.resolve(
                RateLimitRegistry.CHANGE_PASSWORD_USER, userId.toString());
        if (failBucket.getAvailableTokens() <= 0) {
            throw new RateLimitExceededException(
                    "Demasiados intentos fallidos para cambiar la contraseña. Espere antes de volver a intentar.",
                    null);
        }

        User user = loadAuthenticatedUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            // Consume un token solo al fallar: un usuario que cambia su
            // contrasenia correctamente no agota su cuota.
            failBucket.tryConsume(1);
            // 422 (no 401) porque el usuario ESTA autenticado: su JWT fue
            // aceptado por el filtro; lo que falla es la verificacion
            // adicional de identidad sobre el payload. Devolver 401 aqui
            // confundiria a interceptores del FE que disparan auto-logout
            // ante 401.
            throw new IncorrectCurrentPasswordException("La contraseña actual es incorrecta.");
        }

        // El bump invalida todos los JWT previos del usuario; el token nuevo
        // que generamos a continuación lleva un iat posterior y sera el unico
        // aceptado por el filtro hasta la siguiente rotacion.
        Instant now = Instant.now();
        user.changePassword(passwordEncoder.encode(newPassword), now);
        User saved = userRepository.save(user);

        String freshToken = tokenProvider.generateToken(saved);
        return new AuthResult(freshToken, saved);
    }

    @Override
    public void logoutAllSessions(Long userId) {
        User user = loadAuthenticatedUser(userId);
        user.invalidateActiveSessions(Instant.now());
        userRepository.save(user);
    }

    /**
     * Carga al usuario que se obtuvo del contexto de seguridad. Si no se
     * encuentra, lanzamos {@link ResourceNotFoundException} para que el
     * GlobalExceptionHandler responda 404. En la práctica esto sólo ocurre
     * si la cuenta fue eliminada físicamente durante la vida del token.
     */
    private User loadAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", String.valueOf(userId)));
    }
}
