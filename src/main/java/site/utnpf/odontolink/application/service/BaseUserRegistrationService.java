package site.utnpf.odontolink.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.time.LocalDate;

/**
 * Servicio base abstracto para el registro de usuarios.
 * Contiene la lógica común de validación y creación de usuarios.
 * Los servicios específicos (Patient, Practitioner, etc.) extienden de esta clase
 * y proporcionan las validaciones y creación de perfiles específicos de cada rol.
 *
 * Implementa Template Method Pattern para permitir extensibilidad
 * mientras mantiene DRY (Don't Repeat Yourself).
 */
public abstract class BaseUserRegistrationService {

    protected final UserRepository userRepository;
    protected final PasswordEncoder passwordEncoder;

    protected BaseUserRegistrationService(UserRepository userRepository,
                                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Valida las reglas de negocio comunes a todos los tipos de usuario.
     *
     * @param email Email a validar
     * @param dni DNI a validar
     * @throws DuplicateResourceException si el email o DNI ya existen
     */
    protected void validateCommonRules(String email, String dni) {
        // Validación de negocio: El email no debe existir
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Usuario", "email", email);
        }

        // Validación de negocio: El DNI no debe existir
        if (userRepository.existsByDni(dni)) {
            throw new DuplicateResourceException("Usuario", "DNI", dni);
        }
    }

    /**
     * Crea y persiste la entidad User base con los datos comunes.
     *
     * @param email Email del usuario
     * @param password Contraseña en texto plano (será hasheada)
     * @param role Rol a asignar
     * @param firstName Nombre
     * @param lastName Apellido
     * @param dni DNI
     * @param phone Teléfono (puede ser null)
     * @param birthDate Fecha de nacimiento (puede ser null)
     * @return User persistido con ID generado
     */
    protected User createAndSaveUser(String email, String password, Role role,
                                    String firstName, String lastName, String dni,
                                    String phone, LocalDate birthDate) {
        // Crear el User con contraseña hasheada
        User user = new User(
                email,
                passwordEncoder.encode(password),
                role,
                firstName,
                lastName,
                dni,
                phone,
                birthDate
        );

        // Guardar y retornar el User con ID generado
        return userRepository.save(user);
    }

    /**
     * Hook method para validaciones específicas del rol.
     * Los servicios concretos deben implementar este método
     * para agregar validaciones adicionales propias de cada tipo de usuario.
     *
     * Ejemplo: validar que el legajo no exista para practicantes.
     */
    protected abstract void validateRoleSpecificRules();

    /**
     * Hook method para crear el perfil específico del rol.
     * Los servicios concretos deben implementar este método
     * para crear y persistir la entidad específica (Patient, Practitioner, etc.)
     *
     * @param savedUser Usuario base ya persistido
     * @return Entidad específica del rol persistida
     */
    protected abstract Object createAndSaveRoleProfile(User savedUser);
}
