package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAdminUserManagementUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de aplicación que implementa el caso de uso de gestión de
 * usuarios por parte del administrador (RF05).
 *
 * Decisiones de diseño:
 * <ul>
 *   <li>La creación se delega por composición a los casos de uso de
 *       registro existentes ({@code IPatientRegistrationUseCase}, etc.) en
 *       lugar de duplicar la lógica de validación de unicidad y la
 *       creación del perfil específico de cada rol. Esto preserva DRY y
 *       garantiza que cualquier evolución futura (por ejemplo, nuevas
 *       validaciones de DNI) se aplique tanto al auto-registro como al
 *       alta administrativa.</li>
 *   <li>La modificación de perfil ({@link #updateUserProfile}) sigue
 *       restringida a usuarios no-admin: el RF05 no contempla la edición
 *       cruzada entre administradores y ese cambio se trata en un flujo
 *       aparte.</li>
 *   <li>La baja y la reactivación (RF05) sí admiten ahora targets con rol
 *       {@code ROLE_ADMIN}, pero quedan blindadas por dos guards explícitos
 *       contra lockout administrativo (ver
 *       {@link #deactivateUser(Long, User)}): (a) un admin no puede
 *       desactivarse a sí mismo, y (b) no se puede dejar al sistema sin
 *       ningún administrador activo. Esta separación reemplaza al bloqueo
 *       blanket previo, que protegía contra lockout por exceso pero dejaba
 *       al sistema sin forma de retirar admins por la API.</li>
 *   <li>El método {@link #deactivateUser(Long, User)} aprovecha la regla de
 *       negocio ya codificada en {@code User#deactivate()}: lanza
 *       {@code IllegalStateException} si el usuario ya está inactivo, que
 *       se mapea a {@link InvalidBusinessRuleException} para que el
 *       GlobalExceptionHandler responda 422.</li>
 * </ul>
 *
 * Todas las operaciones de escritura son transaccionales para garantizar
 * atomicidad y consistencia con la creación de los perfiles específicos.
 */
@Transactional
public class AdminUserManagementService implements IAdminUserManagementUseCase {

    private final UserRepository userRepository;
    private final IPatientRegistrationUseCase patientRegistrationUseCase;
    private final IPractitionerRegistrationUseCase practitionerRegistrationUseCase;
    private final ISupervisorRegistrationUseCase supervisorRegistrationUseCase;

    public AdminUserManagementService(UserRepository userRepository,
                                      IPatientRegistrationUseCase patientRegistrationUseCase,
                                      IPractitionerRegistrationUseCase practitionerRegistrationUseCase,
                                      ISupervisorRegistrationUseCase supervisorRegistrationUseCase) {
        this.userRepository = userRepository;
        this.patientRegistrationUseCase = patientRegistrationUseCase;
        this.practitionerRegistrationUseCase = practitionerRegistrationUseCase;
        this.supervisorRegistrationUseCase = supervisorRegistrationUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers(Role role, Boolean isActive, String query) {
        // Normalizamos un query vacío a null para que el repositorio pueda
        // saltarse la cláusula LIKE y aprovechar el plan de índice cuando
        // sólo se filtra por rol/estado.
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();
        return userRepository.findAllByFilters(role, isActive, normalizedQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", String.valueOf(id)));
    }

    @Override
    public Patient createPatient(String email, String password, String firstName, String lastName,
                                 String dni, String phone, LocalDate birthDate,
                                 String healthInsurance, String bloodType) {
        return patientRegistrationUseCase.registerPatient(
                email, password, firstName, lastName, dni, phone, birthDate,
                healthInsurance, bloodType
        );
    }

    @Override
    public Practitioner createPractitioner(String email, String password, String firstName, String lastName,
                                           String dni, String phone, LocalDate birthDate,
                                           String studentId, Integer studyYear) {
        return practitionerRegistrationUseCase.registerPractitioner(
                email, password, firstName, lastName, dni, phone, birthDate,
                studentId, studyYear
        );
    }

    @Override
    public Supervisor createSupervisor(String email, String password, String firstName, String lastName,
                                       String dni, String phone, LocalDate birthDate,
                                       String specialty, String employeeId) {
        return supervisorRegistrationUseCase.registerSupervisor(
                email, password, firstName, lastName, dni, phone, birthDate,
                specialty, employeeId
        );
    }

    @Override
    public User updateUserProfile(Long id, String firstName, String lastName,
                                  String phone, LocalDate birthDate) {
        User user = loadExistingUser(id);
        assertProfileEditableByAdmin(user);
        user.updateProfile(firstName, lastName, phone, birthDate);
        return userRepository.save(user);
    }

    @Override
    public User deactivateUser(Long id, User actor) {
        User target = loadExistingUser(id);
        assertNotSelfDeactivation(actor, target);
        assertNotLastActiveAdmin(target);

        try {
            target.deactivate();
        } catch (IllegalStateException ex) {
            // Mapeamos la regla de negocio del dominio a la excepción tipada
            // que entiende el GlobalExceptionHandler para producir 422.
            throw new InvalidBusinessRuleException(ex.getMessage());
        }
        // Sin este bump, un usuario "desactivado" seguiria operando hasta que
        // expire su JWT (24h por default). Invalidar sus sesiones activas en
        // el acto cierra la ventana de abuso post-desactivacion. El filtro
        // JWT del CustomUserDetailsService tambien rechazaria al user inactivo
        // por isActive=false, pero el bump es defensa en profundidad y, sobre
        // todo, evita que tokens en cache (proxies, mobile offline) sigan
        // siendo aceptados por ventanas cortas.
        target.invalidateActiveSessions(Instant.now());
        return userRepository.save(target);
    }

    @Override
    public User reactivateUser(Long id) {
        // Reactivar un admin no necesita guards adicionales: estamos sumando
        // capacidad administrativa, no quitándola. Un admin desactivado no
        // tiene sesión, por lo que tampoco aplica el self-check.
        User user = loadExistingUser(id);
        try {
            user.activate();
        } catch (IllegalStateException ex) {
            throw new InvalidBusinessRuleException(ex.getMessage());
        }
        return userRepository.save(user);
    }

    /**
     * Carga un usuario o lanza 404. No discrimina por rol — los chequeos de
     * autorización contextual viven en los métodos que invocan a este helper.
     */
    private User loadExistingUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", String.valueOf(id)));
    }

    /**
     * El RF05 no contempla la edición cruzada entre administradores. Las
     * bajas/reactivaciones sí (con guards anti-lockout); la edición de
     * perfil queda fuera para no abrir vectores de manipulación entre
     * cuentas de máximo privilegio sin un flujo dedicado.
     */
    private void assertProfileEditableByAdmin(User target) {
        if (target.getRole() == Role.ROLE_ADMIN) {
            throw new InvalidBusinessRuleException(
                    "La edición de perfil administrativa no incluye cuentas de administrador.");
        }
    }

    /**
     * Bloquea el self-lockout: un administrador no puede desactivar su
     * propia cuenta. Es el caso de uso explícito del producto y, además,
     * un patrón estándar de OWASP para evitar que un operador se deje
     * fuera del sistema por error de un solo clic.
     */
    private void assertNotSelfDeactivation(User actor, User target) {
        if (actor != null && actor.getId() != null
                && actor.getId().equals(target.getId())) {
            throw new InvalidBusinessRuleException(
                    "No puedes desactivar tu propia cuenta de administrador.");
        }
    }

    /**
     * Bloquea el total-lockout: si el target es un administrador activo y
     * es el único que queda, la baja dejaría al sistema sin ninguna cuenta
     * administrativa operativa. Recuperarlo requeriría intervenir la base
     * de datos a mano, lo cual viola el principio de operación segura
     * (OWASP ASVS V14.4). Aplicamos sólo si el target tiene rol
     * {@code ROLE_ADMIN} y está actualmente activo: desactivar un admin ya
     * inactivo no cambia el conteo y el chequeo del dominio en
     * {@code User#deactivate()} lo rechazará por su propia regla.
     */
    private void assertNotLastActiveAdmin(User target) {
        if (target.getRole() != Role.ROLE_ADMIN) {
            return;
        }
        if (!target.isActive()) {
            return;
        }
        long activeAdmins = userRepository.countActiveByRole(Role.ROLE_ADMIN);
        if (activeAdmins <= 1) {
            throw new InvalidBusinessRuleException(
                    "No se puede desactivar al único administrador activo del sistema. " +
                    "Cree o reactive otro administrador antes de retirar esta cuenta.");
        }
    }
}
