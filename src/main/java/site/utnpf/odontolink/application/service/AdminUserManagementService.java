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
 *   <li>Las operaciones de modificación, baja y reactivación se restringen
 *       deliberadamente a usuarios de rol PACIENTE, PRACTICANTE o
 *       SUPERVISOR, en línea con el texto del RF05. Bloquear el toque a
 *       otros administradores evita además que un admin pueda dejar al
 *       sistema sin administradores activos por error desde esta API.</li>
 *   <li>El método {@link #deactivateUser(Long)} aprovecha la regla de
 *       negocio ya codificada en {@code User#deactivate()}: lanza
 *       {@code IllegalStateException} si el usuario ya está inactivo, que
 *       se mapea más arriba a {@link InvalidBusinessRuleException}.</li>
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
        User user = loadManageableUser(id);
        user.updateProfile(firstName, lastName, phone, birthDate);
        return userRepository.save(user);
    }

    @Override
    public User deactivateUser(Long id) {
        User user = loadManageableUser(id);
        try {
            user.deactivate();
        } catch (IllegalStateException ex) {
            // Mapeamos la regla de negocio del dominio a la excepción tipada
            // que entiende el GlobalExceptionHandler para producir 422.
            throw new InvalidBusinessRuleException(ex.getMessage());
        }
        return userRepository.save(user);
    }

    @Override
    public User reactivateUser(Long id) {
        User user = loadManageableUser(id);
        try {
            user.activate();
        } catch (IllegalStateException ex) {
            throw new InvalidBusinessRuleException(ex.getMessage());
        }
        return userRepository.save(user);
    }

    /**
     * Carga un usuario asegurándose de que sea gestionable por el
     * administrador desde esta API (paciente, practicante o supervisor).
     * Los usuarios con rol {@link Role#ROLE_ADMIN} quedan deliberadamente
     * fuera del alcance del RF05 y de este servicio.
     */
    private User loadManageableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", String.valueOf(id)));
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new InvalidBusinessRuleException(
                    "La gestión administrativa de usuarios no incluye cuentas de administrador.");
        }
        return user;
    }
}
