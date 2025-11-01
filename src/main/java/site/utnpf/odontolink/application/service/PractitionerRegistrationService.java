package site.utnpf.odontolink.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.time.LocalDate;

/**
 * Servicio de aplicación para el registro de practicantes.
 * Implementa el puerto de entrada IPractitionerRegistrationUseCase.
 * Extiende de BaseUserRegistrationService para reutilizar lógica común.
 *
 * Este servicio se encarga de:
 * - Coordinar el registro de un usuario con rol PRACTITIONER
 * - Aplicar validaciones específicas de practicantes (legajo único)
 * - Crear el perfil específico de Practitioner con studentId y studyYear
 *
 * El bean se registra explícitamente en BeanConfiguration.
 */
@Transactional
public class PractitionerRegistrationService extends BaseUserRegistrationService
        implements IPractitionerRegistrationUseCase {

    private final PractitionerRepository practitionerRepository;

    // Variables de instancia para los datos específicos del practicante
    private String studentId;
    private Integer studyYear;

    public PractitionerRegistrationService(UserRepository userRepository,
                                          PractitionerRepository practitionerRepository,
                                          PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
        this.practitionerRepository = practitionerRepository;
    }

    @Override
    public Practitioner registerPractitioner(String email, String password, String firstName, String lastName,
                                            String dni, String phone, LocalDate birthDate,
                                            String studentId, Integer studyYear) {

        // Guardar datos específicos para uso en hooks
        this.studentId = studentId;
        this.studyYear = studyYear;

        // 1. Validar reglas comunes (email y DNI únicos)
        validateCommonRules(email, dni);

        // 2. Validar reglas específicas del practicante (legajo único)
        validateRoleSpecificRules();

        // 3. Crear y guardar el User base
        User savedUser = createAndSaveUser(
                email,
                password,
                Role.ROLE_PRACTITIONER,
                firstName,
                lastName,
                dni,
                phone,
                birthDate
        );

        // 4. Crear y guardar el perfil específico de Practitioner
        return (Practitioner) createAndSaveRoleProfile(savedUser);
    }

    @Override
    protected void validateRoleSpecificRules() {
        // Validación específica: El legajo (studentId) debe ser único
        if (practitionerRepository.existsByStudentId(studentId)) {
            throw new DuplicateResourceException("Practicante", "legajo", studentId);
        }
    }

    @Override
    protected Practitioner createAndSaveRoleProfile(User savedUser) {
        // Crear el perfil de Practitioner con los datos específicos
        Practitioner practitioner = new Practitioner(savedUser, studentId, studyYear);

        // Guardar y retornar el Practitioner
        return practitionerRepository.save(practitioner);
    }
}
