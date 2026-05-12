package site.utnpf.odontolink.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AdministratorEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAdministratorRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaUserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Crea el primer Administrador del sistema durante el arranque de la
 * aplicación a partir de variables de entorno, una sola vez.
 *
 * Diseño:
 *   - Se activa SÓLO si las tres variables obligatorias están presentes:
 *     odontolink.initial-admin.email
 *     odontolink.initial-admin.password
 *     odontolink.initial-admin.dni
 *     Si alguna falta, el bootstrap se salta sin afectar el arranque.
 *   - Es idempotente: si ya existe un User con ese email, no hace nada.
 *   - La contraseña se hashea en runtime con el PasswordEncoder de Spring
 *     Security (BCrypt), por lo que el hash queda alineado con el que
 *     verifica AuthService en el login. Nada se hardcodea ni se loggea.
 *   - Una vez creado el administrador, basta con borrar las variables de
 *     entorno en la UI de Dokploy: en el próximo deploy el bootstrap
 *     vuelve a saltarse solo.
 *
 * El flujo es transaccional: si falla la creación del perfil Administrator,
 * se hace rollback del User insertado, evitando estados inconsistentes.
 */
@Component
public class InitialAdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrapper.class);

    private final JpaUserRepository userRepository;
    private final JpaAdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${odontolink.initial-admin.email:}")
    private String email;

    @Value("${odontolink.initial-admin.password:}")
    private String password;

    @Value("${odontolink.initial-admin.dni:}")
    private String dni;

    @Value("${odontolink.initial-admin.first-name:}")
    private String firstName;

    @Value("${odontolink.initial-admin.last-name:}")
    private String lastName;

    @Value("${odontolink.initial-admin.phone:}")
    private String phone;

    @Value("${odontolink.initial-admin.birth-date:}")
    private String birthDate;

    public InitialAdminBootstrapper(JpaUserRepository userRepository,
                                    JpaAdministratorRepository administratorRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Switch de apagado: si falta alguna variable obligatoria, no hacemos nada.
        if (isBlank(email) || isBlank(password) || isBlank(dni)) {
            log.info("Initial admin bootstrap skipped: required environment variables are not set");
            return;
        }

        // Idempotencia por email: si ya existe el usuario, no se vuelve a crear.
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() == Role.ROLE_ADMIN
                            && administratorRepository.existsByUser_Id(existing.getId())) {
                        log.info("Initial admin bootstrap skipped: administrator with email '{}' already exists (id={})",
                                email, existing.getId());
                    } else {
                        // Caso anómalo: el email existe pero pertenece a otro rol, o no hay
                        // perfil Administrator asociado. Se loggea sin tocar el dato existente
                        // para no romper integridad referencial.
                        log.warn("Initial admin bootstrap aborted: a user with email '{}' already exists but it is NOT an administrator (role={}). " +
                                        "Resolve the conflict manually before retrying.",
                                email, existing.getRole());
                    }
                },
                this::createAdministrator
        );
    }

    private void createAdministrator() {
        // Defensa adicional: bloquear si el DNI ya está tomado por otro usuario.
        if (userRepository.existsByDni(dni)) {
            log.warn("Initial admin bootstrap aborted: DNI '{}' already belongs to another user", dni);
            return;
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_ADMIN);
        user.setActive(true);
        user.setFirstName(defaultIfBlank(firstName, "Admin"));
        user.setLastName(defaultIfBlank(lastName, "OdontoLink"));
        user.setDni(dni);
        if (!isBlank(phone)) {
            user.setPhone(phone);
        }
        if (!isBlank(birthDate)) {
            try {
                user.setBirthDate(LocalDate.parse(birthDate));
            } catch (DateTimeParseException ex) {
                log.warn("Initial admin bootstrap: birth-date '{}' is not a valid ISO date (expected yyyy-MM-dd). " +
                        "Continuing without birthDate.", birthDate);
            }
        }
        // createdAt y isActive los setea @PrePersist en UserEntity.

        UserEntity savedUser = userRepository.save(user);

        AdministratorEntity admin = new AdministratorEntity();
        admin.setUser(savedUser);
        administratorRepository.save(admin);

        log.info("Initial administrator created successfully: email='{}', userId={}. " +
                        "Remove the INITIAL_ADMIN_* environment variables from Dokploy now.",
                savedUser.getEmail(), savedUser.getId());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
