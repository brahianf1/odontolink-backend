package site.utnpf.odontolink.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IProfileUseCase;
import site.utnpf.odontolink.application.port.in.UpdateProfileCommand;
import site.utnpf.odontolink.domain.exception.AuthenticationFailedException;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

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

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        user.updateSelfProfile(
                requestedEmail,
                command.getFirstName(),
                command.getLastName(),
                command.getPhone(),
                command.getBirthDate(),
                command.getAddress(),
                command.getProfilePictureUrl()
        );

        return userRepository.save(user);
    }

    @Override
    public void changeMyPassword(Long userId, String currentPassword, String newPassword) {
        // La validación de presencia ya la garantiza el DTO con @NotBlank. Aquí
        // sumamos la regla de negocio "la nueva contraseña no puede ser igual
        // a la anterior" para preservar la utilidad de una rotación.
        if (newPassword.equals(currentPassword)) {
            throw new InvalidBusinessRuleException(
                    "La nueva contraseña debe ser distinta de la actual.");
        }

        User user = loadAuthenticatedUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            // Mensaje deliberadamente genérico: no diferenciamos entre "no
            // existe el usuario" y "la contraseña no coincide" para frustrar
            // técnicas de enumeración o de side-channel.
            throw new AuthenticationFailedException("La contraseña actual es incorrecta.");
        }

        user.changePassword(user.getPassword(), passwordEncoder.encode(newPassword));
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
