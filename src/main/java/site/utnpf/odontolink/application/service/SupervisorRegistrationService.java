package site.utnpf.odontolink.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.time.LocalDate;

/**
 * Servicio de aplicación para el registro de supervisores/docentes.
 * Implementa el puerto de entrada ISupervisorRegistrationUseCase.
 * Extiende de BaseUserRegistrationService para reutilizar lógica común.
 *
 * Este servicio se encarga de:
 * - Coordinar el registro de un usuario con rol SUPERVISOR
 * - Aplicar validaciones específicas de supervisores (legajo único)
 * - Crear el perfil específico de Supervisor con specialty y employeeId
 *
 * El bean se registra explícitamente en BeanConfiguration.
 */
@Transactional
public class SupervisorRegistrationService extends BaseUserRegistrationService
        implements ISupervisorRegistrationUseCase {

    private final SupervisorRepository supervisorRepository;

    // Variables de instancia para los datos específicos del supervisor
    private String specialty;
    private String employeeId;

    public SupervisorRegistrationService(UserRepository userRepository,
                                        SupervisorRepository supervisorRepository,
                                        PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
        this.supervisorRepository = supervisorRepository;
    }

    @Override
    public Supervisor registerSupervisor(String email, String password, String firstName, String lastName,
                                        String dni, String phone, LocalDate birthDate,
                                        String specialty, String employeeId) {

        // Guardar datos específicos para uso en hooks
        this.specialty = specialty;
        this.employeeId = employeeId;

        // 1. Validar reglas comunes (email y DNI únicos)
        validateCommonRules(email, dni);

        // 2. Validar reglas específicas del supervisor (legajo único)
        validateRoleSpecificRules();

        // 3. Crear y guardar el User base
        User savedUser = createAndSaveUser(
                email,
                password,
                Role.ROLE_SUPERVISOR,
                firstName,
                lastName,
                dni,
                phone,
                birthDate
        );

        // 4. Crear y guardar el perfil específico de Supervisor
        return (Supervisor) createAndSaveRoleProfile(savedUser);
    }

    @Override
    protected void validateRoleSpecificRules() {
        // Validación específica: El legajo docente (employeeId) debe ser único
        if (supervisorRepository.existsByEmployeeId(employeeId)) {
            throw new DuplicateResourceException("Supervisor", "legajo", employeeId);
        }
    }

    @Override
    protected Supervisor createAndSaveRoleProfile(User savedUser) {
        // Crear el perfil de Supervisor con los datos específicos
        Supervisor supervisor = new Supervisor(savedUser, specialty, employeeId);

        // Guardar y retornar el Supervisor
        return supervisorRepository.save(supervisor);
    }
}
